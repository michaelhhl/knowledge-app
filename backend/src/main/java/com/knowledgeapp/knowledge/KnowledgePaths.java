package com.knowledgeapp.knowledge;

import com.knowledgeapp.api.ApiException;
import com.knowledgeapp.api.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** Path traversal and symlink escape protection for knowledge reads (06-architecture.md). */
public final class KnowledgePaths {

    private static final Pattern DRIVE_LETTER = Pattern.compile("^[A-Za-z]:.*");
    private static final String INVALID_MESSAGE = "文档路径无效。";

    private KnowledgePaths() {
    }

    /** Validates a client-supplied relative path and returns the normalized '/' form. */
    public static String normalizeRelative(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || relativePath.indexOf('\0') >= 0) {
            throw invalid();
        }
        String path = relativePath.strip().replace('\\', '/');
        if (path.startsWith("/") || DRIVE_LETTER.matcher(path).matches() || path.contains(":")) {
            throw invalid();
        }
        StringBuilder normalized = new StringBuilder();
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                throw invalid();
            }
            if (normalized.length() > 0) {
                normalized.append('/');
            }
            normalized.append(segment);
        }
        if (normalized.length() == 0 || !normalized.toString().endsWith(".md")) {
            throw invalid();
        }
        return normalized.toString();
    }

    /** Resolves a normalized relative path to a real file inside the real knowledge root. */
    public static Path resolveExisting(Path knowledgeRoot, String normalizedRelative) {
        try {
            Path realRoot = knowledgeRoot.toRealPath();
            Path candidate = realRoot.resolve(normalizedRelative).normalize();
            if (!candidate.startsWith(realRoot)) {
                throw invalid();
            }
            if (!Files.isRegularFile(candidate)) {
                throw new ApiException(ErrorCode.DOC_NOT_FOUND, "文档不存在或未发布。");
            }
            Path real = candidate.toRealPath();
            if (!real.startsWith(realRoot)) {
                throw invalid();
            }
            return real;
        } catch (InvalidPathException ex) {
            throw invalid();
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.DOC_NOT_FOUND, "文档不存在或未发布。");
        }
    }

    private static ApiException invalid() {
        return new ApiException(ErrorCode.INVALID_PATH, INVALID_MESSAGE);
    }
}
