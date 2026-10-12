package com.knowledgeapp.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String knowledgeRoot,
        String indexDir,
        Llm llm,
        Qa qa,
        Retrieval retrieval) {

    public record Llm(
            String provider,
            String model,
            String apiKey,
            String baseUrl,
            @DefaultValue("90s") Duration timeout) {

        @Override
        public String toString() {
            return "Llm[provider=" + provider + ", model=" + model + ", apiKey=***]";
        }
    }

    public record Qa(
            @DefaultValue("true") boolean showRetrieval,
            @DefaultValue("8") int topK,
            @DefaultValue("10") int maxHistoryTurns) {
    }

    public record Retrieval(@DefaultValue("0.5") double minScore) {
    }
}
