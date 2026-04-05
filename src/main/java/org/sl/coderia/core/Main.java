package org.sl.coderia.core;

import dev.langchain4j.model.chat.ChatModel;
import org.sl.coderia.core.agent.Assistant;
import org.sl.coderia.core.agent.ChatModelFactory;
import org.sl.coderia.core.sandbox.EnvironmentSnapshot;
import org.sl.coderia.core.sandbox.ToolSandbox;
import org.sl.coderia.core.tools.Tools;
import org.sl.coderia.core.utils.args.ArgForModel;
import org.sl.coderia.core.utils.args.ArgParser;
import org.sl.coderia.core.utils.TerminalIO;
import org.sl.coderia.core.utils.Utils;

import java.util.Arrays;
import java.util.HashSet;

public class Main {

    public static void main(String[] args) throws Exception {

        ArgParser argParser = new ArgParser(args);
        ArgForModel argForModel = argParser.parse(ArgForModel.class);
        TerminalIO.getInstance().printLine(argForModel.toString());
        ChatModel model = ChatModelFactory.openAiChatModelBuilder()
                .baseUrl(argForModel.getBaseUrl())
                .apiKey(argForModel.getApiKey())
                .modelName(argForModel.getModelName())
                .httpClientBuilder(Utils.createHttpClientBuilder(argForModel.getTimeout()))
                .build();
        ToolSandbox toolSandbox = new ToolSandbox(new HashSet<>(Arrays.stream(ToolSandbox.Permission.values()).toList()));
        Assistant assistant = ChatModelFactory.createAssistant(model, argForModel.getMaxMemoryMessages(), new Tools(toolSandbox));
        TerminalIO terminal = TerminalIO.getInstance();
        EnvironmentSnapshot snapshot;
        String memoryId = java.util.UUID.randomUUID().toString();

        while (true) {
            String question = terminal.read("You: ");
            if (question == null || question.isBlank() || question.equalsIgnoreCase("exit")) {
                break;
            }

            snapshot = EnvironmentSnapshot.ofCurrent();
            String response = assistant.ask(memoryId, snapshot.render(), question);
            TerminalIO.getInstance().printLine("Assistant: " + response);
        }
    }

}
