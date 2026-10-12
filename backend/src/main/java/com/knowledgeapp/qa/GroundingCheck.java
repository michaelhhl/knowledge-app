package com.knowledgeapp.qa;

import com.knowledgeapp.index.Chunk;
import com.knowledgeapp.index.SearchHit;
import com.knowledgeapp.qa.QaDtos.Citation;
import com.knowledgeapp.qa.QaDtos.Related;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Drops citations outside the provided chunks; grounded without citations becomes insufficient. */
@Component
public class GroundingCheck {

    static final String INSUFFICIENT_TEXT =
            "当前知识库里没有足够依据回答这个问题。我不能靠猜测补全。你可以补充文档，或换一种问法（例如具体流程名称）。";
    static final String REFUSED_TEXT = "无法协助该请求。";
    private static final int MAX_RELATED = 3;

    public record Checked(AnswerStatus status, String markdown, List<Citation> citations, List<Related> related) {
    }

    public Checked check(ModelAnswer answer, List<SearchHit> provided, List<SearchHit> allHits) {
        return switch (answer.status()) {
            case REFUSED -> new Checked(AnswerStatus.REFUSED,
                    answer.markdown().isBlank() ? REFUSED_TEXT : answer.markdown().strip(),
                    List.of(), List.of());
            case INSUFFICIENT -> insufficient(allHits);
            case GROUNDED -> {
                List<Citation> citations = validCitations(answer.citations(), provided);
                if (citations.isEmpty() || answer.markdown().isBlank()) {
                    yield insufficient(allHits);
                }
                yield new Checked(AnswerStatus.GROUNDED, answer.markdown().strip(), citations, List.of());
            }
        };
    }

    public Checked insufficient(List<SearchHit> allHits) {
        return new Checked(AnswerStatus.INSUFFICIENT, INSUFFICIENT_TEXT, List.of(), related(allHits));
    }

    private static List<Citation> validCitations(List<ModelAnswer.ModelCitation> cited, List<SearchHit> provided) {
        Map<String, Citation> result = new LinkedHashMap<>();
        for (ModelAnswer.ModelCitation c : cited) {
            SearchHit match = findMatch(c, provided);
            if (match == null) {
                continue;
            }
            Chunk chunk = match.chunk();
            String key = chunk.file() + "#" + chunk.heading();
            result.putIfAbsent(key, new Citation(chunk.file(), chunk.heading(), chunk.title(), match.score()));
        }
        return new ArrayList<>(result.values());
    }

    private static SearchHit findMatch(ModelAnswer.ModelCitation c, List<SearchHit> provided) {
        SearchHit fileOnly = null;
        for (SearchHit hit : provided) {
            Chunk chunk = hit.chunk();
            if (!chunk.file().equals(c.file())) {
                continue;
            }
            if (chunk.heading().equals(c.heading())) {
                return hit;
            }
            if (fileOnly == null && chunk.headingPath().contains(c.heading())) {
                fileOnly = hit;
            }
        }
        return fileOnly;
    }

    private static List<Related> related(List<SearchHit> allHits) {
        Set<String> seen = new LinkedHashSet<>();
        List<Related> related = new ArrayList<>();
        for (SearchHit hit : allHits) {
            if (seen.add(hit.chunk().file())) {
                related.add(new Related(hit.chunk().file(), hit.chunk().title()));
                if (related.size() == MAX_RELATED) {
                    break;
                }
            }
        }
        return related;
    }
}
