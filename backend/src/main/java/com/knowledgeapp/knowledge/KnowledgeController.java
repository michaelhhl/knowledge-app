package com.knowledgeapp.knowledge;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/toc")
    public KnowledgeService.Toc toc() {
        return knowledgeService.toc();
    }

    @GetMapping("/docs")
    public List<DocumentInfo> docs() {
        return knowledgeService.documents();
    }

    @GetMapping("/doc")
    public KnowledgeService.DocumentView doc(@RequestParam("path") String path) {
        return knowledgeService.read(path);
    }
}
