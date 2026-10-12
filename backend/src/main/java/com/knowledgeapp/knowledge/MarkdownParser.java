package com.knowledgeapp.knowledge;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Parses front matter and title per 04-knowledge-model.md. */
public final class MarkdownParser {

    private MarkdownParser() {
    }

    public static KnowledgeDocument parse(String relativePath, String rawText) {
        String text = rawText.replace("\r\n", "\n").replace('\r', '\n');
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }

        Map<?, ?> meta = Map.of();
        String body = text;
        if (text.startsWith("---\n")) {
            int end = findFrontMatterEnd(text);
            if (end > 0) {
                meta = loadYaml(text.substring(4, end));
                int bodyStart = text.indexOf('\n', end + 1);
                body = bodyStart < 0 ? "" : text.substring(bodyStart + 1);
            }
        }

        String fileStem = stem(relativePath);
        String id = stringValue(meta.get("id"));
        String title = stringValue(meta.get("title"));
        if (title == null) {
            title = firstH1(body);
        }
        if (title == null) {
            title = fileStem;
        }
        String status = stringValue(meta.get("status"));
        status = status == null ? KnowledgeDocument.STATUS_PUBLISHED : status.trim().toLowerCase(Locale.ROOT);

        return new KnowledgeDocument(
                relativePath,
                id != null ? id : "kb-" + fileStem,
                title,
                tags(meta.get("tags")),
                dateValue(meta.get("updated")),
                status,
                body);
    }

    private static int findFrontMatterEnd(String text) {
        int pos = 4;
        while (pos < text.length()) {
            int lineEnd = text.indexOf('\n', pos);
            String line = lineEnd < 0 ? text.substring(pos) : text.substring(pos, lineEnd);
            if (line.strip().equals("---")) {
                return pos;
            }
            if (lineEnd < 0) {
                return -1;
            }
            pos = lineEnd + 1;
        }
        return -1;
    }

    private static Map<?, ?> loadYaml(String yamlText) {
        try {
            Object loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(yamlText);
            return loaded instanceof Map<?, ?> map ? map : Map.of();
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private static String firstH1(String body) {
        boolean inFence = false;
        for (String line : body.split("\n", -1)) {
            if (isFence(line)) {
                inFence = !inFence;
            } else if (!inFence && line.startsWith("# ")) {
                return line.substring(2).strip();
            }
        }
        return null;
    }

    static boolean isFence(String line) {
        String trimmed = line.stripLeading();
        return trimmed.startsWith("```") || trimmed.startsWith("~~~");
    }

    private static String stem(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.endsWith(".md") ? name.substring(0, name.length() - 3) : name;
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString().strip();
        return s.isEmpty() ? null : s;
    }

    private static String dateValue(Object value) {
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString();
        }
        return stringValue(value);
    }

    private static List<String> tags(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof Collection<?> items) {
            for (Object item : items) {
                String s = stringValue(item);
                if (s != null) {
                    result.add(s);
                }
            }
        } else if (value != null) {
            Arrays.stream(value.toString().split(","))
                    .map(String::strip)
                    .filter(s -> !s.isEmpty())
                    .forEach(result::add);
        }
        return List.copyOf(result);
    }
}
