package org.sl.coderia.core.agent;

import dev.langchain4j.model.chat.ChatModel;
import org.sl.coderia.core.tools.*;
import org.sl.coderia.core.tools.action.CommandAction;
import org.sl.coderia.core.tools.action.FileAction;
import org.sl.coderia.core.tools.action.InteractiveAction;
import org.sl.coderia.core.utils.TerminalIO;
import org.sl.coderia.core.utils.Utils;
import org.sl.coderia.core.utils.args.CommandLineArgs;
import org.sl.coderia.core.utils.args.ArgParser;

import java.util.*;
import java.util.stream.Collectors;



public class Agent {



    public void run(String[] args) throws Exception {
        TerminalIO terminal = TerminalIO.getInstance();
        terminal.printLine("Welcome to Coderia!");

        ArgParser argParser = new ArgParser(args);
        CommandLineArgs commandLineArgs = argParser.parse(CommandLineArgs.class);

        Set<ToolSandbox.Permission> permissions = Arrays.stream(ToolSandbox.Permission.values()).collect(Collectors.toSet());
        ToolSandbox toolSandbox = new ToolSandbox(permissions);
        ToolRegistry toolRegistry = ToolRegistry.withTools(new InteractiveAction(),new CommandAction(),new FileAction());
        ToolExecutor toolExecutor = new ToolExecutor(toolSandbox, toolRegistry);

        AgentLoop agentLoop = AgentLoop.builder()
                .model(buildModel(commandLineArgs))
                .tools(toolExecutor)
                .terminal(TerminalIO.getInstance())
                .build();

        while (true) {

            String question = terminal.read("You: ");
            if (question == null || question.isBlank() || question.equalsIgnoreCase("exit")) {
                break;
            }
            agentLoop.run(question);
        }
    }
    private static ChatModel buildModel(CommandLineArgs commandLineArgs) {
        ChatModel model = ChatModelFactory.openAiChatModelBuilder()
                .baseUrl(commandLineArgs.getBaseUrl())
                .apiKey(commandLineArgs.getApiKey())
                .modelName(commandLineArgs.getModel())
                .httpClientBuilder(Utils.createHttpClientBuilder(commandLineArgs.getTimeout()))
                .build();
        if (commandLineArgs.isUseOllama()) {
            model = ChatModelFactory.ollamaChatModelBuilder()
                    .baseUrl(commandLineArgs.getBaseUrl())
                    .modelName(commandLineArgs.getModel())
                    .httpClientBuilder(Utils.createHttpClientBuilder(commandLineArgs.getTimeout()))
                    .build();

        }
        return model;
    }
}
