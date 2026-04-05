package org.sl.coderia.core;

import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.*;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

public class Main {
    interface Assistant {

        @SystemMessage("""
            You are a precise coding agent. Think step-by-step before acting.
            
            ## Core rules
            - **Always use tools** for anything retrievable.
            - **Ground every claim** — if you assert a fact, you must have observed it via
              a tool or the conversation. Flag uncertainty explicitly ("I haven't verified…").
            - **One tool at a time** — call a tool, inspect its output fully, then decide
              the next action. Do not chain calls without processing intermediate results.
            Always use memory.md to persist you plan read it necessay
            Environment:
            {{context}}
            
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
            """)
        String ask(@MemoryId String memoryId, @V("context") String context  , @UserMessage String question);
    }

    public static void main(String[] args) throws IOException {
        HttpClientBuilder httpClientBuilder = JdkHttpClient.builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(120))       // local models are slow
                .httpClientBuilder(java.net.http.HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1));

        ChatModel model = OpenAiChatModel.builder()
                .apiKey("sk-")
                .baseUrl("http://192.168.1.17:1234/v1")
                .modelName("qwen/qwen3-coder-30b")
                .logRequests(true)
                .logResponses(true)
                .httpClientBuilder(httpClientBuilder)
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(10)
                        .chatMemoryStore(new InMemoryChatMemoryStore())
                        .build())
                .tools(new Tools())
                .build();

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
