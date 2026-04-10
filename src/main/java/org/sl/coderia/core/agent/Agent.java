package org.sl.coderia.core.agent;

import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.ChatModel;
import org.sl.coderia.core.tools.ToolExecutor;
import org.sl.coderia.core.tools.ToolSandbox;
import org.sl.coderia.core.tools.Tools;
import org.sl.coderia.core.utils.TerminalIO;
import org.sl.coderia.core.utils.Utils;
import org.sl.coderia.core.utils.args.CommandLineArgs;
import org.sl.coderia.core.utils.args.ArgParser;

import java.util.*;
import java.util.stream.Collectors;



public class Agent {



    public void run(String[] args) throws Exception {
        TerminalIO terminal = TerminalIO.getInstance();
        ArgParser argParser = new ArgParser(args);
        CommandLineArgs commandLineArgs = argParser.parse(CommandLineArgs.class);
        System.out.println(commandLineArgs);

        Set<ToolSandbox.Permission> permissions = Arrays.stream(ToolSandbox.Permission.values()).collect(Collectors.toSet());
        Tools tools = new Tools(new ToolSandbox(permissions));
        AgentLoop agentLoop = AgentLoop.builder()
                .model(buildModel(commandLineArgs))
                .tools(ToolExecutor.withTools(tools))
                .specs(ToolSpecifications.toolSpecificationsFrom(tools))
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
