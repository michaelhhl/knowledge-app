package com.knowledgeapp.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Resolves KNOWLEDGE_ROOT and INDEX_DIR defaults as described in 06-architecture.md. */
@Component
public class AppPaths {

    private final Path knowledgeRoot;
    private final Path indexDir;

    public AppPaths(AppProperties properties) {
        this.knowledgeRoot = resolveKnowledgeRoot(properties.knowledgeRoot());
        this.indexDir = StringUtils.hasText(properties.indexDir())
                ? Path.of(properties.indexDir()).toAbsolutePath().normalize()
                : defaultIndexDir(knowledgeRoot);
    }

    public Path knowledgeRoot() {
        return knowledgeRoot;
    }

    public Path indexDir() {
        return indexDir;
    }

    private static Path resolveKnowledgeRoot(String configured) {
        if (StringUtils.hasText(configured)) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        Path cwd = Path.of("").toAbsolutePath();
        for (Path candidate : new Path[] {cwd.resolve("knowledge"), cwd.resolveSibling("knowledge")}) {
            if (Files.isDirectory(candidate)) {
                return candidate.normalize();
            }
        }
        return cwd.resolve("knowledge").normalize();
    }

    private static Path defaultIndexDir(Path knowledgeRoot) {
        Path parent = knowledgeRoot.getParent() != null ? knowledgeRoot.getParent() : knowledgeRoot;
        return parent.resolve(".local").resolve("index");
    }
}
