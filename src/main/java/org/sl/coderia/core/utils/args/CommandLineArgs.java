package org.sl.coderia.core.utils.args;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class CommandLineArgs implements Helper {

    @Arg(name = "api-key", description = "OpenAI API key", defaultValue = "sk-")
    private String apiKey;
    @Arg(name = "--url", description = "OpenAI API URL", defaultValue = "http://localhost:1234/v1")
    private String baseUrl;
    @Arg(name = "--model", description = "OpenAI model name", defaultValue = "qwen/qwen3-coder-30b")
    private String model;
    @Arg(name = "--timeout", description = "OpenAI API timeout", defaultValue = "10")
    private long timeout;
    @Arg(name = "--max-memory-messages", description = "Maximum number of messages to store in memory", defaultValue = "10")
    private int maxMemoryMessages;
    @Arg(name = "--log-requests", description = "Log requests to stdout", defaultValue = "false")
    private boolean logRequests;
    @Arg(name = "--log-responses", description = "Log responses from OpenAI to stdout", defaultValue = "false")
    private boolean logResponse;
    @Arg(name = "--use-ollama", description = "Use Ollama instead of OpenAI", defaultValue = "false")
    private boolean useOllama = false;
    @Arg(name = "--use-openai", description = "Use OpenAI instead of Ollama", defaultValue = "true")
    private boolean useOpenAi = true;


    @SuppressWarnings("unused")
    public CommandLineArgs() {
        // Needed for reflection
    }


    @Override
    public String help() {
        return """
                Available arguments:
                  --api-key             OpenAI API key
                  --url                 OpenAI API URL
                  --model               OpenAI model name
                  --timeout             OpenAI API timeout
                  --max-memory-messages Maximum number of messages to store in memory
                  --log-requests        Log requests to stdout
                  --log-responses       Log responses from OpenAI to stdout
                  --use-ollama          Use Ollama instead of OpenAI
                  --use-openai          Use OpenAI instead of Ollama
                """;
    }
}
