package com.knowledgeapp.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgeapp.index.IndexManager;
import com.knowledgeapp.index.SearchHit;
import com.knowledgeapp.retrieval.Retriever;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Retrieval-level regression over the real knowledge/ using docs/fixtures/qa-eval.md. */
@SpringBootTest
@DirtiesContext
class RetrievalEvalTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        registry.add("app.knowledge-root", EvalCase.KNOWLEDGE_ROOT::toString);
        registry.add("app.index-dir", () -> tempIndexDir());
        registry.add("app.llm.provider", () -> "");
    }

    private static String tempIndexDir() {
        try {
            return Files.createTempDirectory("knowledge-app-eval-index").toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Autowired
    Retriever retriever;

    @Autowired
    IndexManager indexManager;

    static Stream<EvalCase> cases() {
        return EvalCase.load().stream();
    }

    @Test
    void evalFileHasCases() {
        assertThat(EvalCase.load()).hasSizeGreaterThanOrEqualTo(5);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void retrievalMatchesExpectation(EvalCase c) {
        Retriever.Result result = retriever.retrieve(c.question(), 8);
        List<String> relevantFiles = result.relevant().stream().map(h -> h.chunk().file()).toList();
        String scores = result.all().stream()
                .map(h -> h.chunk().file() + "#" + h.chunk().heading() + "=" + String.format("%.2f", h.score()))
                .reduce((a, b) -> a + ", " + b).orElse("(none)");

        if (c.expectedStatus().equals("grounded")) {
            assertThat(relevantFiles).as("hits: %s", scores).contains(c.mustCite());
        }
        if (c.outOfScope()) {
            assertThat(result.relevant()).as("hits: %s", scores).isEmpty();
        }
    }

    @Test
    void indexContainsOnlyPublishedDocuments() {
        assertThat(indexManager.documents()).allSatisfy(d -> assertThat(d.path()).doesNotStartWith("_"));
        List<SearchHit> any = indexManager.search("签证", 50);
        assertThat(any).allSatisfy(h -> assertThat(h.chunk().file()).doesNotStartWith("_"));
    }
}
