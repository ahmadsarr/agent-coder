package org.sl.coderia.core.utils.args;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CommandLineArgs implements Helper {
    private static final String DEFAULT_API_KEY = "sk-";
    private static final String DEFAULT_BASE_URL = "http://localhost:1234/v1";
    private static final String DEFAULT_MODEL_NAME = "qwen/qwen3-coder-30b";
    private static final String DEFAULT_CONNECT_TIMEOUT_SECONDS = "10";
    private static final String DEFAULT_MAX_MEMORY_MESSAGES = "10";
    private static final String DEFAULT_LOG_REQUESTS = "true";
    private static final String DEFAULT_LOG_RESPONSES = "true";
    @Arg(name = "--api-key", description = "OpenAI API key",defaultValue = DEFAULT_API_KEY)
    private String apiKey;
    @Arg(name = "--url", description = "OpenAI API URL",defaultValue = DEFAULT_BASE_URL)
    private String baseUrl;
    @Arg(name = "--model", description = "OpenAI model name",defaultValue = DEFAULT_MODEL_NAME)
    private String modelName;
    @Arg(name = "--timeout", description = "OpenAI API timeout",defaultValue = DEFAULT_CONNECT_TIMEOUT_SECONDS )
    private long timeout;
    @Arg(name = "--max-memory-messages", description = "Maximum number of messages to store in memory",defaultValue = DEFAULT_MAX_MEMORY_MESSAGES)
    private int maxMemoryMessages;
    @Arg(name = "--log-requests", description = "Log requests to stdout",defaultValue = DEFAULT_LOG_REQUESTS)
    private boolean logRequests;
    @Arg(name = "--log-responses", description = "Log responses from OpenAI to stdout",defaultValue = DEFAULT_LOG_RESPONSES)
    private boolean logResponse;

    public CommandLineArgs() {}

    @Override
    public String toString() {
        return "ArgForModel{" +
                "apiKey='" + apiKey + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", modelName='" + modelName + '\'' +
                ", timeout=" + timeout +
                ", maxMemoryMessages=" + maxMemoryMessages +
                ", logRequests=" + logRequests +
                ", logResponse=" + logResponse +
                '}';
    }

    @Override
    public String help() {
        return """
                Available arguments:
                  --api-key             OpenAI API key (default: %s)
                  --url                 OpenAI API URL (default: %s)
                  --model               OpenAI model name (default: %s)
                  --timeout             OpenAI API timeout (default: %S)
                  --max-memory-messages Maximum number of messages to store in memory (default: %S)
                  --log-requests        Log requests to stdout (default: %s)
                  --log-responses       Log responses from OpenAI to stdout (default: %s)
                """.formatted(
                DEFAULT_API_KEY,
                DEFAULT_BASE_URL,
                DEFAULT_MODEL_NAME,
                DEFAULT_CONNECT_TIMEOUT_SECONDS,
                DEFAULT_MAX_MEMORY_MESSAGES,
                DEFAULT_LOG_REQUESTS,
                DEFAULT_LOG_RESPONSES
        );
    }
}
