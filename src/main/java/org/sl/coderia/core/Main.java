package org.sl.coderia.core;

import dev.langchain4j.model.chat.ChatModel;
import org.sl.coderia.core.agent.Assistant;
import org.sl.coderia.core.agent.ChatModelFactory;
import org.sl.coderia.core.sandbox.EnvironmentSnapshot;
import org.sl.coderia.core.sandbox.ToolSandbox;
import org.sl.coderia.core.tools.Tools;
import org.sl.coderia.core.utils.Constants.MODEL;
import org.sl.coderia.core.utils.ArgParser;
import org.sl.coderia.core.utils.TerminalIO;
import org.sl.coderia.core.utils.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public class Main {

    public static void main(String[] args) throws IOException {

        ArgParser argParser = new ArgParser(args);
        String apiKey = argParser.getStringValue("--api-key", MODEL.DEFAULT_API_KEY);
        String baseUrl = argParser.getStringValue("--url", MODEL.DEFAULT_BASE_URL);
        String modelName = argParser.getStringValue("--model", MODEL.DEFAULT_MODEL_NAME);
        long timeout = argParser.getLongValue("--timeout", MODEL.DEFAULT_CONNECT_TIMEOUT_SECONDS);
        int maxMemoryMessages = argParser.getIntValue("--max-memory-messages", MODEL.DEFAULT_MAX_MEMORY_MESSAGES);
        boolean logRequests = argParser.getBooleanValue("--log-requests", MODEL.DEFAULT_LOG_REQUESTS);
        boolean logResponses = argParser.getBooleanValue("--log-responses", MODEL.DEFAULT_LOG_RESPONSES);
        System.out.println("Parameters:");
        System.out.println("apiKey: " +apiKey);
        System.out.println("baseUrl: "+ baseUrl);
        System.out.println("modelName: "+ modelName);
        System.out.println("timeout: "+ timeout);
        System.out.println("maxMemoryMessages: "+ maxMemoryMessages);
        System.out.println("logRequests: "+ logRequests);
        System.out.println("logResponses: "+ logResponses);
        System.out.println("----------------------------------------");
        ChatModel model = ChatModelFactory.create(apiKey, baseUrl, modelName, logRequests, logResponses, Utils.createHttpClientBuilder(timeout));
        ToolSandbox toolSandbox = new ToolSandbox(new HashSet<>(Arrays.stream(ToolSandbox.Permission.values()).toList()));
        Assistant assistant = ChatModelFactory.createAssistant(model, maxMemoryMessages, new Tools(toolSandbox));
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
            System.out.println("Assistant: " + response);
        }
    }

}
