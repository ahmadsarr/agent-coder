package org.sl.coderia.core.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import lombok.Builder;
import org.sl.coderia.core.tools.ToolExecutor;
import org.sl.coderia.core.utils.TerminalIO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AgentLoop {
    private static final int DEFAULT_MAX_ITERATIONS = 15;

    private final ChatModel model;
    private final ToolExecutor tools;
    private final TerminalIO terminal;
    private final List<ChatMessage> trajectory;
    private final List<ToolSpecification> specs;
    private final Integer maxIterations;


    @Builder
    public AgentLoop(ChatModel model, ToolExecutor tools, TerminalIO terminal, List<ToolSpecification> specs, Integer maxIterations) {
        this.model = model;
        this.tools = tools;
        this.terminal = terminal;
        this.specs = specs;
        this.trajectory = new ArrayList<>();
        trajectory.add(SystemMessage.from(prompt()));
        this.maxIterations = maxIterations == null || maxIterations == 0 ? DEFAULT_MAX_ITERATIONS : maxIterations;
    }

    public void run(String question) {

        trajectory.set(0, SystemMessage.from(prompt()));
        trajectory.add(UserMessage.from(question));
        trajectory.add(SystemMessage.from("### Your Memory plan" + readMemory()));


        boolean done = false;

        for (int i = 0; i < this.maxIterations && !done; i++) {
            compressObservations(2000, 10);
            ChatResponse response = model.chat(
                    ChatRequest.builder()
                            .messages(trajectory)
                            .toolSpecifications(specs)
                            .build()
            );
            AiMessage msg = response.aiMessage();

            if (response.finishReason().equals(FinishReason.STOP) || !msg.hasToolExecutionRequests())
                return;
            String text = msg.text();
           if(text!=null && text.isBlank())
               terminal.printLine("Assistant: " + text);
            trajectory.add(msg);
            ToolExecutionRequest req = msg.toolExecutionRequests().get(0);
            ToolExecutionResultMessage observation = tools.execute(req);
            trajectory.add(observation);

        }
    }

    private void compressObservations(int maxChars, int maxMessages) {

        trajectory.replaceAll(msg -> {
            if (msg instanceof ToolExecutionResultMessage t && t.text() != null) {
                if (t.toolName().equals("readFileRaw") || t.text().length() <= maxChars) {
                    return msg;
                }
                return ToolExecutionResultMessage.builder()
                        .id(t.id())
                        .toolName(t.toolName())
                        .text(t.text().substring(0, maxChars) + "\n…[truncated]")
                        .build();
            }
            return msg;
        });

        List<Integer> toolIndexes = new ArrayList<>();

        for (int i = 0; i < trajectory.size(); i++) {
            if (trajectory.get(i) instanceof ToolExecutionResultMessage) {
                toolIndexes.add(i);
            }
        }

        int excess = toolIndexes.size() - maxMessages;

        for (int i = 0; i < excess; i++) {
            int indexToRemove = toolIndexes.get(i);
            trajectory.remove(indexToRemove - i);
        }
    }

    public String readMemory() {
        try {
            Path path = Path.of("memory.md");
            if (!Files.exists(path)) return "";
            return Files.readString(path);
        } catch (Exception e) {
            return "";
        }
    }

    public String prompt() {
        return """
                You are a precise coding agent.
                
                ## Core rules
                - **Always use tools** for anything retrievable.
                - **Ground every claim** — if you assert a fact, you must have observed it via
                  a tool or the conversation. Flag uncertainty explicitly ("I haven't verified…").
                - **One tool at a time** — call a tool, inspect its output fully, then decide
                  the next action. Do not chain calls without processing intermediate results.
                Always use memory.md to persist you plan read it necessay
                
                ## Output format
                - Lead with the direct answer or code.
                - Follow with a brief reasoning trace only if non-obvious.
                - If a tool call failed or returned nothing useful, say so and explain how you
                  proceeded without it.
                """;
    }
}
