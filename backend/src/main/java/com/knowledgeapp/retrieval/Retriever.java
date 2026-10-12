package com.knowledgeapp.retrieval;

import com.knowledgeapp.config.AppProperties;
import com.knowledgeapp.index.IndexManager;
import com.knowledgeapp.index.SearchHit;
import java.util.List;
import org.springframework.stereotype.Component;

/** top-k with a score threshold; drafts are never indexed, so never returned. */
@Component
public class Retriever {

    private final IndexManager indexManager;
    private final double minScore;

    public Retriever(IndexManager indexManager, AppProperties properties) {
        this.indexManager = indexManager;
        this.minScore = properties.retrieval().minScore();
    }

    public record Result(List<SearchHit> relevant, List<SearchHit> all) {
    }

    public Result retrieve(String query, int topK) {
        List<SearchHit> all = indexManager.search(query, topK);
        List<SearchHit> relevant = all.stream().filter(h -> h.score() >= minScore).toList();
        return new Result(relevant, all);
    }
}
