package com.knowledgeapp.qa;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgeapp.index.Chunk;
import com.knowledgeapp.index.SearchHit;
import com.knowledgeapp.qa.ModelAnswer.ModelCitation;
import java.util.List;
import org.junit.jupiter.api.Test;

class GroundingCheckTest {

    private final GroundingCheck check = new GroundingCheck();

    private static SearchHit hit(String file, String title, String heading, double score) {
        return new SearchHit(new Chunk(file + "#0", file, "kb-x", title, List.of(title, heading), List.of(), "text"),
                score);
    }

    private final List<SearchHit> provided = List.of(
            hit("visa.md", "签证", "照片", 3.2),
            hit("visa.md", "签证", "护照", 2.1));

    @Test
    void groundedWithoutCitationsBecomesInsufficient() {
        var result = check.check(new ModelAnswer(AnswerStatus.GROUNDED, "照片要白底。", List.of()), provided, provided);

        assertThat(result.status()).isEqualTo(AnswerStatus.INSUFFICIENT);
        assertThat(result.markdown()).isEqualTo(GroundingCheck.INSUFFICIENT_TEXT);
        assertThat(result.citations()).isEmpty();
        assertThat(result.related()).extracting(QaDtos.Related::file).containsExactly("visa.md");
    }

    @Test
    void citationsOutsideProvidedChunksAreDropped() {
        var answer = new ModelAnswer(AnswerStatus.GROUNDED, "答案", List.of(
                new ModelCitation("other.md", "照片"),
                new ModelCitation("visa.md", "不存在的节")));

        assertThat(check.check(answer, provided, provided).status()).isEqualTo(AnswerStatus.INSUFFICIENT);
    }

    @Test
    void validCitationsAreKeptWithScoreAndDeduplicated() {
        var answer = new ModelAnswer(AnswerStatus.GROUNDED, " 答案 ", List.of(
                new ModelCitation("visa.md", "照片"),
                new ModelCitation("visa.md", "照片"),
                new ModelCitation("fake.md", "x")));

        var result = check.check(answer, provided, provided);

        assertThat(result.status()).isEqualTo(AnswerStatus.GROUNDED);
        assertThat(result.markdown()).isEqualTo("答案");
        assertThat(result.citations()).singleElement().satisfies(c -> {
            assertThat(c.file()).isEqualTo("visa.md");
            assertThat(c.heading()).isEqualTo("照片");
            assertThat(c.title()).isEqualTo("签证");
            assertThat(c.score()).isEqualTo(3.2);
        });
    }

    @Test
    void citingDocumentTitleMatchesAChunkOfThatFile() {
        var answer = new ModelAnswer(AnswerStatus.GROUNDED, "答案", List.of(new ModelCitation("visa.md", "签证")));
        assertThat(check.check(answer, provided, provided).citations()).hasSize(1);
    }

    @Test
    void blankGroundedAnswerBecomesInsufficient() {
        var answer = new ModelAnswer(AnswerStatus.GROUNDED, "  ", List.of(new ModelCitation("visa.md", "照片")));
        assertThat(check.check(answer, provided, provided).status()).isEqualTo(AnswerStatus.INSUFFICIENT);
    }

    @Test
    void insufficientUsesFixedTextAndNoCitations() {
        var answer = new ModelAnswer(AnswerStatus.INSUFFICIENT, "模型自己的话", List.of(new ModelCitation("visa.md", "照片")));
        var result = check.check(answer, provided, provided);
        assertThat(result.markdown()).isEqualTo(GroundingCheck.INSUFFICIENT_TEXT);
        assertThat(result.citations()).isEmpty();
    }

    @Test
    void refusedHasNoCitationsOrRelated() {
        var answer = new ModelAnswer(AnswerStatus.REFUSED, "", List.of(new ModelCitation("visa.md", "照片")));
        var result = check.check(answer, provided, provided);
        assertThat(result.status()).isEqualTo(AnswerStatus.REFUSED);
        assertThat(result.markdown()).isEqualTo(GroundingCheck.REFUSED_TEXT);
        assertThat(result.citations()).isEmpty();
        assertThat(result.related()).isEmpty();
    }
}
