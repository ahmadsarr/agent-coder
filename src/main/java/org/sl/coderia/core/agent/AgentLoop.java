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
import lombok.Builder;
import org.sl.coderia.core.tools.ToolExecutor;
import org.sl.coderia.core.utils.TerminalIO;

import java.util.ArrayList;
import java.util.List;

public class AgentLoop {

    private final ChatModel model;
    private final ToolExecutor tools;
    private final TerminalIO terminal;
    private final List<ChatMessage> trajectory;
    private final List<ToolSpecification> specs;


    @Builder
    public AgentLoop(ChatModel model, ToolExecutor tools, TerminalIO terminal,List<ToolSpecification> specs) {
        this.model = model;
        this.tools = tools;
        this.terminal = terminal;
        this.specs = specs;
        this.trajectory = new ArrayList<>();
        trajectory.add(SystemMessage.from(prompt()));

    }

    public void run(String question) {
        trajectory.add(UserMessage.from(question));

        boolean done = false;

        for (int i = 0; i < 10 && !done; i++) {
            ChatResponse response = model.chat(
                    ChatRequest.builder()
                            .messages(trajectory)
                            .toolSpecifications(specs)
                            .build()
            );

            AiMessage msg = response.aiMessage();
            trajectory.add(msg);

            if (msg.hasToolExecutionRequests()) {
                for (ToolExecutionRequest req : msg.toolExecutionRequests()) {

                    ToolExecutionResultMessage observation = tools.execute(req);
                    terminal.printLine("Observation: " + observation.text());
                    trajectory.add(observation);

                    if ("finish".equals(req.name())) {
                        done = true;
                        break;
                    }
                }
            } else {
                done = true;
            }

            terminal.printLine("Assistant: " + response.aiMessage().text());
        }
    }

    public String prompt() {
        return """
                You are a precise coding agent. Think step-by-step before acting.

                ## Core rules
                - **Always use tools** for anything retrievable.
                - **Ground every claim** — if you assert a fact, you must have observed it via
                  a tool or the conversation. Flag uncertainty explicitly ("I haven't verified…").
                - **One tool at a time** — call a tool, inspect its output fully, then decide
                  the next action. Do not chain calls without processing intermediate results.
                Always use memory.md to persist you plan read it necessay

                ## Decision loop
                THOUGHT  → What do I know? What is still unknown?
                ACTION   → Which tool resolves the unknown? Call it.
                OBSERVE  → Read the full output. Update your understanding.
                REPEAT   → Until the answer is fully grounded.
                ANSWER   → Respond concisely. Cite which tools confirmed each key fact.

                ## Output format
                - Lead with the direct answer or code.
                - Follow with a brief reasoning trace only if non-obvious.
                - If a tool call failed or returned nothing useful, say so and explain how you
                  proceeded without it.
                """;
    }
}
