package org.sl.coderia.core;

import org.sl.coderia.core.agent.Agent;

public class Main {

    public static void main(String[] args) throws Exception {
/*
        ArgParser argParser = new ArgParser(args);
        CommandLineArgs commandLineArgs = argParser.parse(CommandLineArgs.class);
        TerminalIO.getInstance().printLine(commandLineArgs.toString());
        ChatModel model = ChatModelFactory.openAiChatModelBuilder()
                .baseUrl(commandLineArgs.getBaseUrl())
                .apiKey(commandLineArgs.getApiKey())
                .modelName(commandLineArgs.getModelName())
                .httpClientBuilder(Utils.createHttpClientBuilder(commandLineArgs.getTimeout()))
                .build();
        ToolSandbox toolSandbox = new ToolSandbox(new HashSet<>(Arrays.stream(ToolSandbox.Permission.values()).toList()));
        Assistant assistant = ChatModelFactory.createAssistant(model, commandLineArgs.getMaxMemoryMessages(), new Tools(toolSandbox));
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
        }*/
        new Agent().run(args);
    }

}
