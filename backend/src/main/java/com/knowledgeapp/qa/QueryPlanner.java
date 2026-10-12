package com.knowledgeapp.qa;

import com.knowledgeapp.qa.QaDtos.HistoryTurn;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Rule-based query rewrite for follow-ups (06-architecture.md, FR-08). Never adds facts. */
@Component
public class QueryPlanner {

    private static final Pattern ANAPHORA = Pattern.compile(
            "这个|那个|这份|那份|上一份|上一个|这些|那些|上面|刚才|它|\\b(this|that|it|these|those)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final int SHORT_QUESTION_LENGTH = 6;

    public String plan(String question, List<HistoryTurn> history) {
        String q = question.strip();
        if (history.isEmpty()) {
            return q;
        }
        boolean followUp = q.codePointCount(0, q.length()) < SHORT_QUESTION_LENGTH || ANAPHORA.matcher(q).find();
        if (!followUp) {
            return q;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            String previous = history.get(i).question();
            if (previous != null && !previous.isBlank()) {
                return previous.strip() + " " + q;
            }
        }
        return q;
    }
}
