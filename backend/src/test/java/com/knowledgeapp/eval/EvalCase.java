package com.knowledgeapp.eval;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** One row of docs/fixtures/qa-eval.md. */
public record EvalCase(
        String question,
        String expectedStatus,
        String mustCite,
        List<String> forbidden,
        boolean outOfScope) {

    static final Path REPO_ROOT = Path.of("").toAbsolutePath().getParent();
    static final Path EVAL_FILE = REPO_ROOT.resolve("docs/fixtures/qa-eval.md");
    static final Path KNOWLEDGE_ROOT = REPO_ROOT.resolve("knowledge");

    static List<EvalCase> load() {
        List<String> lines;
        try {
            lines = Files.readAllLines(EVAL_FILE, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        List<EvalCase> cases = new ArrayList<>();
        boolean inTable = false;
        for (String line : lines) {
            String trimmed = line.strip();
            if (!trimmed.startsWith("|")) {
                inTable = false;
                continue;
            }
            List<String> cells = Arrays.stream(trimmed.substring(1, trimmed.length() - 1).split("\\|", -1))
                    .map(String::strip)
                    .toList();
            if (cells.get(0).equals("问题")) {
                inTable = true;
                continue;
            }
            if (!inTable || cells.get(0).startsWith("---") || cells.size() < 5) {
                continue;
            }
            List<String> forbidden = cells.get(3).isEmpty()
                    ? List.of()
                    : Arrays.stream(cells.get(3).split("、")).map(String::strip).toList();
            cases.add(new EvalCase(cells.get(0), cells.get(1), cells.get(2), forbidden,
                    cells.get(4).contains("检索无命中")));
        }
        return cases;
    }

    @Override
    public String toString() {
        return question;
    }
}
