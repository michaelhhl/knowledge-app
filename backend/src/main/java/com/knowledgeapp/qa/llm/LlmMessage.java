package com.knowledgeapp.qa.llm;

public record LlmMessage(String role, String content) {

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content);
    }
}
