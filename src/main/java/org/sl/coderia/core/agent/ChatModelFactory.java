package org.sl.coderia.core.agent;

import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.Builder;

import java.time.Duration;

public final class ChatModelFactory {

    private ChatModelFactory() {
    }

    @Builder( builderMethodName = "openAiChatModelBuilder")
    public static ChatModel createOpenAiChatModel(
            String apiKey,
            String baseUrl,
            String modelName,
            boolean logRequests,
            boolean logResponses,
            HttpClientBuilder httpClientBuilder
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.0)
                .timeout(Duration.ofMinutes(10))
                .logRequests(logRequests)
                .logResponses(logResponses)
                .httpClientBuilder(httpClientBuilder)
                .build();
    }

    @Builder( builderMethodName = "ollamaChatModelBuilder")
    public static ChatModel OllamaChatModel(
            String baseUrl,
            String modelName,
            boolean logRequests,
            boolean logResponses,
            HttpClientBuilder httpClientBuilder
    ) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.0)
                .numCtx(32768)        // sinon l'agent perd le contexte très vite
                .logRequests(logRequests)
                .logResponses(logResponses)
                .httpClientBuilder(httpClientBuilder)
                .build();
    }



    public static Assistant createAssistant(ChatModel model, int maxMemoryMessages, Object...tools) {
        return AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(maxMemoryMessages)
                        .chatMemoryStore(new InMemoryChatMemoryStore())
                        .build())
                .tools(tools)
                .build();
    }
}
