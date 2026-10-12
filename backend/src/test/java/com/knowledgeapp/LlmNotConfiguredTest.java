package com.knowledgeapp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** U-01: without LLM config, browsing and indexing work and Q&A returns a readable error. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class LlmNotConfiguredTest {

    static final Path ROOT = TestKnowledge.create();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.knowledge-root", ROOT::toString);
        registry.add("app.index-dir", () -> ROOT.resolveSibling(".local").resolve("index").toString());
        registry.add("app.llm.provider", () -> "");
        registry.add("app.llm.model", () -> "");
        registry.add("app.llm.api-key", () -> "");
        registry.add("app.llm.base-url", () -> "");
    }

    @Autowired
    MockMvc mvc;

    @Test
    void askReturnsConfigurationError() throws Exception {
        mvc.perform(post("/api/qa/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"年假怎么申请？\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("LLM_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.message").value("未配置 LLM。请复制 .env.example 为 .env 并填写密钥。"));
        mvc.perform(get("/api/qa/status")).andExpect(jsonPath("$.llm_configured").value(false));
    }

    @Test
    void browsingAndIndexStillWork() throws Exception {
        mvc.perform(get("/api/knowledge/toc")).andExpect(status().isOk());
        mvc.perform(get("/api/knowledge/doc").param("path", "it/vpn.md")).andExpect(status().isOk());
        mvc.perform(get("/api/index/status")).andExpect(jsonPath("$.state").value("READY"));
    }
}
