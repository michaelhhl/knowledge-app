package com.knowledgeapp.index;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/index")
public class IndexController {

    private final IndexManager indexManager;

    public IndexController(IndexManager indexManager) {
        this.indexManager = indexManager;
    }

    @GetMapping("/status")
    public IndexStatus status() {
        return indexManager.status();
    }

    @PostMapping("/rebuild")
    public IndexStatus rebuild() {
        return indexManager.rebuild();
    }
}
