package com.knowledgeapp.qa;

import com.knowledgeapp.api.ApiException;
import com.knowledgeapp.api.ErrorCode;
import com.knowledgeapp.config.AppProperties;
import com.knowledgeapp.index.IndexManager;
import com.knowledgeapp.index.SearchHit;
import com.knowledgeapp.qa.QaDtos.AskResponse;
import com.knowledgeapp.qa.QaDtos.HistoryTurn;
import com.knowledgeapp.qa.QaDtos.RetrievalHit;
import com.knowledgeapp.qa.llm.LlmClient;
import com.knowledgeapp.qa.llm.LlmClientSource;
import com.knowledgeapp.qa.llm.LlmException;
import com.knowledgeapp.retrieval.Retriever;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class QaService {

    static final String LLM_NOT_CONFIGURED_MESSAGE = "未配置 LLM。请复制 .env.example 为 .env 并填写密钥。";

    private final LlmClientSource llmClientSource;
    private final IndexManager indexManager;
    private final QueryPlanner queryPlanner;
    private final Retriever retriever;
    private final AnswerComposer answerComposer;
    private final GroundingCheck groundingCheck;
    private final AppProperties.Qa qa;

    public QaService(
            LlmClientSource llmClientSource,
            IndexManager indexManager,
            QueryPlanner queryPlanner,
            Retriever retriever,
            AnswerComposer answerComposer,
            GroundingCheck groundingCheck,
            AppProperties properties) {
        this.llmClientSource = llmClientSource;
        this.indexManager = indexManager;
        this.queryPlanner = queryPlanner;
        this.retriever = retriever;
        this.answerComposer = answerComposer;
        this.groundingCheck = groundingCheck;
        this.qa = properties.qa();
    }

    public AskResponse ask(String question, List<HistoryTurn> history) {
        LlmClient client = llmClientSource.client()
                .orElseThrow(() -> new ApiException(ErrorCode.LLM_NOT_CONFIGURED, LLM_NOT_CONFIGURED_MESSAGE));
        if (!indexManager.isReady()) {
            throw new ApiException(ErrorCode.INDEX_NOT_READY, "索引尚未建立，请先重建索引。");
        }

        String query = queryPlanner.plan(question, history);
        Retriever.Result retrieval = retriever.retrieve(query, qa.topK());

        GroundingCheck.Checked checked;
        if (retrieval.relevant().isEmpty()) {
            checked = groundingCheck.insufficient(retrieval.all());
        } else {
            ModelAnswer answer;
            try {
                answer = answerComposer.compose(client, question, history, retrieval.relevant());
            } catch (LlmException ex) {
                throw new ApiException(ErrorCode.LLM_ERROR, ex.getMessage(), ex);
            }
            checked = groundingCheck.check(answer, retrieval.relevant(), retrieval.all());
        }

        return new AskResponse(
                checked.status(),
                checked.markdown(),
                checked.citations(),
                checked.related(),
                qa.showRetrieval() ? summarize(retrieval.all()) : List.of());
    }

    private static List<RetrievalHit> summarize(List<SearchHit> hits) {
        return hits.stream()
                .map(h -> new RetrievalHit(h.chunk().file(), h.chunk().heading(), h.score()))
                .toList();
    }
}
