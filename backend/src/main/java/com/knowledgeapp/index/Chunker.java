package com.knowledgeapp.index;

import com.knowledgeapp.knowledge.KnowledgeDocument;
import java.util.ArrayList;
import java.util.List;

/** Splits documents per the chunk rules in 04-knowledge-model.md. */
public final class Chunker {

    static final int MAX_CJK_CHARS = 1200;
    static final int MAX_LATIN_WORDS = 800;

    private Chunker() {
    }

    public static List<Chunk> chunk(KnowledgeDocument doc) {
        if (!doc.published()) {
            return List.of();
        }
        List<Section> sections = splitSections(doc.body(), "## ", null);
        List<Chunk> chunks = new ArrayList<>();
        for (Section section : sections) {
            List<String> basePath = section.heading == null
                    ? List.of(doc.title())
                    : List.of(doc.title(), section.heading);
            if (section.heading != null && isTooLong(section.text)) {
                for (Section sub : splitSections(section.text, "### ", section.heading)) {
                    List<String> path = sub.heading == null || sub.heading.equals(section.heading)
                            ? basePath
                            : List.of(doc.title(), section.heading, sub.heading);
                    addChunk(chunks, doc, path, sub.text);
                }
            } else {
                addChunk(chunks, doc, basePath, section.text);
            }
        }
        return chunks;
    }

    private static void addChunk(List<Chunk> chunks, KnowledgeDocument doc, List<String> path, String text) {
        String trimmed = stripH1(text).strip();
        if (trimmed.isEmpty()) {
            return;
        }
        chunks.add(new Chunk(
                doc.path() + "#" + chunks.size(),
                doc.path(),
                doc.id(),
                doc.title(),
                path,
                doc.tags(),
                trimmed));
    }

    private record Section(String heading, String text) {
    }

    /** Splits on the given ATX prefix, ignoring headings inside code fences. */
    private static List<Section> splitSections(String body, String prefix, String leadingHeading) {
        List<Section> sections = new ArrayList<>();
        String currentHeading = leadingHeading;
        StringBuilder current = new StringBuilder();
        boolean inFence = false;
        boolean first = true;
        for (String line : body.split("\n", -1)) {
            if (isFence(line)) {
                inFence = !inFence;
            }
            if (!inFence && line.startsWith(prefix)) {
                if (!first || current.length() > 0) {
                    sections.add(new Section(currentHeading, current.toString()));
                }
                currentHeading = line.substring(prefix.length()).strip();
                current = new StringBuilder();
                first = false;
                continue;
            }
            current.append(line).append('\n');
        }
        sections.add(new Section(currentHeading, current.toString()));
        return sections;
    }

    private static String stripH1(String text) {
        StringBuilder out = new StringBuilder();
        boolean inFence = false;
        for (String line : text.split("\n", -1)) {
            if (isFence(line)) {
                inFence = !inFence;
            }
            if (!inFence && line.startsWith("# ")) {
                continue;
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private static boolean isFence(String line) {
        String t = line.stripLeading();
        return t.startsWith("```") || t.startsWith("~~~");
    }

    static boolean isTooLong(String text) {
        int cjk = 0;
        int words = 0;
        boolean inWord = false;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (isCjk(cp)) {
                cjk++;
                inWord = false;
            } else if (Character.isLetterOrDigit(cp)) {
                if (!inWord) {
                    words++;
                    inWord = true;
                }
            } else {
                inWord = false;
            }
        }
        return cjk > MAX_CJK_CHARS || words > MAX_LATIN_WORDS;
    }

    private static boolean isCjk(int cp) {
        Character.UnicodeScript script = Character.UnicodeScript.of(cp);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }
}
