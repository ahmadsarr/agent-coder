package org.sl.coderia.core.utils;

import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClient;

import java.net.http.HttpClient;
import java.time.Duration;

public class Utils {
    private Utils() {}

    public static HttpClientBuilder createHttpClientBuilder(long timeout) {
        return JdkHttpClient.builder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .readTimeout(Duration.ofSeconds(timeout * 10L))
                .httpClientBuilder(java.net.http.HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1));
    }
}
