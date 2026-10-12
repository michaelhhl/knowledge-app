package com.knowledgeapp.qa;

import java.util.List;

/** Parsed model output before GroundingCheck. */
public record ModelAnswer(AnswerStatus status, String markdown, List<ModelCitation> citations) {

    public record ModelCitation(String file, String heading) {
    }
}
