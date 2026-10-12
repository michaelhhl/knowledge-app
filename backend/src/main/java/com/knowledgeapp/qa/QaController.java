package com.knowledgeapp.qa;

import com.knowledgeapp.qa.QaDtos.AskRequest;
import com.knowledgeapp.qa.QaDtos.AskResponse;
import com.knowledgeapp.qa.QaDtos.QaStatus;
import com.knowledgeapp.qa.llm.LlmClientSource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qa")
public class QaController {

    private final QaService qaService;
    private final LlmClientSource llmClientSource;

    public QaController(QaService qaService, LlmClientSource llmClientSource) {
        this.qaService = qaService;
        this.llmClientSource = llmClientSource;
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return qaService.ask(request.question(), request.historyOrEmpty());
    }

    @GetMapping("/status")
    public QaStatus status() {
        return new QaStatus(llmClientSource.client().isPresent());
    }
}
