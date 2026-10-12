package com.knowledgeapp.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgeapp.qa.AnswerStatus;
import com.knowledgeapp.qa.QaDtos.AskResponse;
import com.knowledgeapp.qa.QaService;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** End-to-end eval against a real model; runs only when LLM_PROVIDER is set in the environment. */
@SpringBootTest
@DirtiesContext
@EnabledIfEnvironmentVariable(named = "LLM_PROVIDER", matches = ".+")
class QaEvalTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        String indexDir = Files.createTempDirectory("knowledge-app-qa-eval-index").toString();
        registry.add("app.knowledge-root", EvalCase.KNOWLEDGE_ROOT::toString);
        registry.add("app.index-dir", () -> indexDir);
    }

    @Autowired
    QaService qaService;

    static Stream<EvalCase> cases() {
        return EvalCase.load().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void answerMatchesExpectation(EvalCase c) {
        AskResponse response = qaService.ask(c.question(), List.of());

        assertThat(response.answerStatus()).as(response.answerMarkdown())
                .isEqualTo(AnswerStatus.fromWire(c.expectedStatus()));
        if (!c.mustCite().isEmpty()) {
            assertThat(response.citations()).extracting(cit -> cit.file()).contains(c.mustCite());
        }
        for (String phrase : c.forbidden()) {
            assertThat(response.answerMarkdown()).doesNotContain(phrase);
        }
    }
}
