package com.knowledgeapp.qa;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Wire format for /api/qa, field names follow 05-ai-qa-rules.md. */
public final class QaDtos {

    private QaDtos() {
    }

    public record AskRequest(
            @NotBlank(message = "不能为空") @Size(max = 2000, message = "不能超过 2000 字") String question,
            @Valid @Size(max = 50) List<HistoryTurn> history) {

        public List<HistoryTurn> historyOrEmpty() {
            return history == null ? List.of() : history;
        }
    }

    public record HistoryTurn(
            @Size(max = 2000) String question,
            @Size(max = 20000) String answer) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AskResponse(
            AnswerStatus answerStatus,
            String answerMarkdown,
            List<Citation> citations,
            List<Related> related,
            List<RetrievalHit> retrieval) {
    }

    public record Citation(String file, String heading, String title, double score) {
    }

    public record Related(String file, String title) {
    }

    public record RetrievalHit(String file, String heading, double score) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record QaStatus(boolean llmConfigured) {
    }
}
