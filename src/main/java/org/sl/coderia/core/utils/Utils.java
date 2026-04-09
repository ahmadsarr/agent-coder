package org.sl.coderia.core.utils;

import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClient;

import java.net.http.HttpClient;
import java.time.Duration;

public class Utils {
    private Utils() {
    }

    public static HttpClientBuilder createHttpClientBuilder(long timeoutSeconds) {
        System.out.println("Timeout: " + timeoutSeconds + "s");
        return JdkHttpClient.builder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds*2))
                .httpClientBuilder(java.net.http.HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1));
    }

    public static String escapeJson(String s) {
        return s.replace("\"", "\\\"").replace("\n", "\\n");
    }

}
