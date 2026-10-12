package com.knowledgeapp.knowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts Markdown links from _index.md. */
public final class TocParser {

    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\(([^)\\s]+)\\)");

    private TocParser() {
    }

    public record TocLink(String title, String target) {
    }

    public static List<TocLink> parse(String indexMarkdown) {
        List<TocLink> links = new ArrayList<>();
        Matcher m = LINK.matcher(indexMarkdown);
        while (m.find()) {
            String target = m.group(2);
            int hash = target.indexOf('#');
            if (hash >= 0) {
                target = target.substring(0, hash);
            }
            if (target.startsWith("./")) {
                target = target.substring(2);
            }
            if (!target.isEmpty() && !target.contains("://")) {
                links.add(new TocLink(m.group(1).strip(), target));
            }
        }
        return links;
    }
}
