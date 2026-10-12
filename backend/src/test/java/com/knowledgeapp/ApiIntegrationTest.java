package com.knowledgeapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.knowledgeapp.qa.llm.LlmClient;
import com.knowledgeapp.qa.llm.LlmClientSource;
import com.knowledgeapp.qa.llm.LlmException;
import com.knowledgeapp.qa.llm.LlmMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class ApiIntegrationTest {

    static final Path ROOT = TestKnowledge.create();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.knowledge-root", ROOT::toString);
        registry.add("app.index-dir", () -> ROOT.resolveSibling(".local").resolve("index").toString());
        registry.add("app.llm.provider", () -> "");
    }

    /** Scriptable LLM; records the last prompt so tests can assert what was sent. */
    static class FakeLlm implements LlmClient {
        final AtomicReference<Function<List<LlmMessage>, String>> responder = new AtomicReference<>();
        final AtomicReference<List<LlmMessage>> lastMessages = new AtomicReference<>();

        @Override
        public String complete(List<LlmMessage> messages) {
            lastMessages.set(messages);
            return responder.get().apply(messages);
        }
    }

    @TestConfiguration
    static class FakeLlmConfig {
        @Bean
        FakeLlm fakeLlm() {
            return new FakeLlm();
        }

        @Bean
        @Primary
        LlmClientSource fakeLlmSource(FakeLlm fakeLlm) {
            return () -> Optional.of(fakeLlm);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeLlm fakeLlm;

    @BeforeEach
    void reset() {
        fakeLlm.lastMessages.set(null);
        fakeLlm.responder.set(m -> {
            throw new AssertionError("LLM should not be called");
        });
    }

    @Test
    void tocListsPublishedIndexEntriesAndOthers() throws Exception {
        mvc.perform(get("/api/knowledge/toc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(false))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].path").value("leave-policy.md"))
                .andExpect(jsonPath("$.items[0].title").value("请假流程"))
                .andExpect(jsonPath("$.others[0].path").value("it/vpn.md"));
    }

    @Test
    void docsExcludeDraftsAndMetaFiles() throws Exception {
        mvc.perform(get("/api/knowledge/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].path").value(not(hasItem("secret-draft.md"))))
                .andExpect(jsonPath("$[*].path").value(not(hasItem("_index.md"))))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void readsPublishedDocumentWithoutFrontMatter() throws Exception {
        mvc.perform(get("/api/knowledge/doc").param("path", "leave-policy.md"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("请假流程"))
                .andExpect(jsonPath("$.tags[0]").value("hr"))
                .andExpect(jsonPath("$.markdown").value(containsString("## 年假申请")))
                .andExpect(jsonPath("$.markdown").value(not(containsString("status:"))));
    }

    @Test
    void rejectsTraversalAndHidesDraftsAndMetaFiles() throws Exception {
        mvc.perform(get("/api/knowledge/doc").param("path", "../outside.md"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PATH"));
        mvc.perform(get("/api/knowledge/doc").param("path", ROOT.resolveSibling("outside.md").toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PATH"));
        mvc.perform(get("/api/knowledge/doc").param("path", "secret-draft.md"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOC_NOT_FOUND"));
        mvc.perform(get("/api/knowledge/doc").param("path", "_index.md"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOC_NOT_FOUND"));
    }

    @Test
    void unknownApiPathReturnsErrorShape() throws Exception {
        mvc.perform(get("/api/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void groundedAnswerKeepsOnlyProvidedCitations() throws Exception {
        fakeLlm.responder.set(m -> """
                {"answer_status": "grounded",
                 "answer_markdown": "年假需提前三个工作日提交。",
                 "citations": [{"file": "leave-policy.md", "heading": "年假申请"},
                               {"file": "invented.md", "heading": "编造"}]}
                """);

        mvc.perform(post("/api/qa/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"年假要提前几天申请？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer_status").value("grounded"))
                .andExpect(jsonPath("$.answer_markdown").value("年假需提前三个工作日提交。"))
                .andExpect(jsonPath("$.citations.length()").value(1))
                .andExpect(jsonPath("$.citations[0].file").value("leave-policy.md"))
                .andExpect(jsonPath("$.citations[0].heading").value("年假申请"))
                .andExpect(jsonPath("$.citations[0].title").value("请假流程"))
                .andExpect(jsonPath("$.retrieval[0].file").value("leave-policy.md"));

        String prompt = fakeLlm.lastMessages.get().get(1).content();
        assertThat(prompt).contains("提前三个工作日").doesNotContain("紫色独角兽").doesNotContain("不应被读取");
    }

    @Test
    void groundedWithoutValidCitationIsDowngraded() throws Exception {
        fakeLlm.responder.set(m -> "{\"answer_status\":\"grounded\",\"answer_markdown\":\"编的\",\"citations\":[]}");

        mvc.perform(post("/api/qa/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"病假需要什么证明？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer_status").value("insufficient"))
                .andExpect(jsonPath("$.answer_markdown").value(containsString("当前知识库里没有足够依据")))
                .andExpect(jsonPath("$.citations").value(empty()))
                .andExpect(jsonPath("$.related[0].file").value("leave-policy.md"));
    }

    @Test
    void outOfScopeQuestionIsInsufficientWithoutCallingModel() throws Exception {
        mvc.perform(post("/api/qa/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"火星的首都是哪里？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer_status").value("insufficient"))
                .andExpect(jsonPath("$.citations").value(empty()));
        assertThat(fakeLlm.lastMessages.get()).isNull();
    }

    @Test
    void draftContentIsNeverRetrieved() throws Exception {
        mvc.perform(post("/api/qa/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"紫色独角兽项目的预算是多少？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer_status").value("insufficient"))
                .andExpect(jsonPath("$.retrieval[*].file").value(not(hasItem("secret-draft.md"))))
                .andExpect(jsonPath("$.related[*].file").value(not(hasItem("secret-draft.md"))));
    }

    @Test
    void followUpUsesHistoryForRetrieval() throws Exception {
        fakeLlm.responder.set(m -> """
                {"answer_status":"grounded","answer_markdown":"由直属主管审批。",
                 "citations":[{"file":"leave-policy.md","heading":"年假申请"}]}
                """);

        mvc.perform(post("/api/qa/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "这个谁审批？",
                                 "history": [{"question": "年假怎么申请？", "answer": "提前三个工作日提交。"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer_status").value("grounded"));
        assertThat(fakeLlm.lastMessages.get().get(1).content()).contains("<对话历史>", "年假怎么申请？");
    }

    @Test
    void modelFailuresAreReadableErrors() throws Exception {
        fakeLlm.responder.set(m -> "not json at all");
        mvc.perform(post("/api/qa/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"年假怎么申请？\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("LLM_INVALID_RESPONSE"));

        fakeLlm.responder.set(m -> {
            throw new LlmException("无法连接模型服务。");
        });
        mvc.perform(post("/api/qa/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"年假怎么申请？\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("LLM_ERROR"))
                .andExpect(jsonPath("$.message").value("无法连接模型服务。"));
    }

    @Test
    void blankQuestionIsRejected() throws Exception {
        mvc.perform(post("/api/qa/ask").contentType(MediaType.APPLICATION_JSON).content("{\"question\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(post("/api/qa/ask").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void qaStatusReportsConfiguredWithoutSecrets() throws Exception {
        mvc.perform(get("/api/qa/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.llm_configured").value(true))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rebuildPicksUpChangesAndStatusReportsStaleness() throws Exception {
        mvc.perform(get("/api/index/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("READY"))
                .andExpect(jsonPath("$.stale").value(false));

        Path added = ROOT.resolve("expense.md");
        Files.writeString(added, "---\ntitle: 报销\n---\n# 报销\n\n## 差旅报销\n差旅发票需在月底前提交财务。\n");
        try {
            mvc.perform(get("/api/index/status")).andExpect(jsonPath("$.stale").value(true));
            mvc.perform(get("/api/knowledge/doc").param("path", "expense.md")).andExpect(status().isNotFound());

            mvc.perform(post("/api/index/rebuild"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state").value("READY"))
                    .andExpect(jsonPath("$.stale").value(false))
                    .andExpect(jsonPath("$.documentCount").value(3));
            mvc.perform(get("/api/knowledge/doc").param("path", "expense.md")).andExpect(status().isOk());
            fakeLlm.responder.set(m -> "{\"answer_status\":\"insufficient\",\"answer_markdown\":\"\",\"citations\":[]}");
            mvc.perform(post("/api/qa/ask").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\": \"差旅发票什么时候提交？\"}"))
                    .andExpect(jsonPath("$.retrieval[0].file").value("expense.md"));
        } finally {
            Files.deleteIfExists(added);
            mvc.perform(post("/api/index/rebuild")).andExpect(status().isOk());
        }
    }
}
