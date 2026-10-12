package com.knowledgeapp.index;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgeapp.knowledge.KnowledgeDocument;
import com.knowledgeapp.knowledge.MarkdownParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkerTest {

    @Test
    void splitsOnH2WithMetadata() {
        List<Chunk> chunks = Chunker.chunk(MarkdownParser.parse("guide.md", """
                ---
                id: kb-guide
                title: 指南
                tags: [faq]
                ---
                # 指南

                简介段落。

                ## 第一节
                内容一。

                ## 第二节
                内容二。
                """));

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).headingPath()).containsExactly("指南");
        assertThat(chunks.get(0).text()).isEqualTo("简介段落。");
        assertThat(chunks.get(1).headingPath()).containsExactly("指南", "第一节");
        assertThat(chunks.get(1).heading()).isEqualTo("第一节");
        assertThat(chunks.get(1).text()).isEqualTo("内容一。");
        assertThat(chunks.get(2).file()).isEqualTo("guide.md");
        assertThat(chunks.get(2).docId()).isEqualTo("kb-guide");
        assertThat(chunks.get(2).tags()).containsExactly("faq");
    }

    @Test
    void skipsEmptyIntro() {
        List<Chunk> chunks = Chunker.chunk(MarkdownParser.parse("a.md", "# 标题\n\n## 节\n内容\n"));
        assertThat(chunks).extracting(Chunk::heading).containsExactly("节");
    }

    @Test
    void ignoresHeadingsInsideCodeFences() {
        List<Chunk> chunks = Chunker.chunk(MarkdownParser.parse("a.md", """
                # T
                ## 命令
                ```bash
                ## 这不是标题
                echo hi
                ```
                """));
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).contains("## 这不是标题");
    }

    @Test
    void splitsLongSectionOnH3() {
        String longText = "很".repeat(Chunker.MAX_CJK_CHARS + 10);
        List<Chunk> chunks = Chunker.chunk(MarkdownParser.parse("a.md",
                "# T\n## 长节\n前言\n### 子一\n" + longText + "\n### 子二\n短\n"));

        assertThat(chunks).extracting(Chunk::headingPath).containsExactly(
                List.of("T", "长节"),
                List.of("T", "长节", "子一"),
                List.of("T", "长节", "子二"));
    }

    @Test
    void keepsShortSectionWithH3Together() {
        List<Chunk> chunks = Chunker.chunk(MarkdownParser.parse("a.md", "# T\n## 节\n### 子\n短\n"));
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).contains("### 子");
    }

    @Test
    void draftProducesNoChunks() {
        KnowledgeDocument draft = MarkdownParser.parse("d.md", "---\nstatus: draft\n---\n# D\n## 节\n秘密\n");
        assertThat(Chunker.chunk(draft)).isEmpty();
    }

    @Test
    void measuresLatinWords() {
        assertThat(Chunker.isTooLong("word ".repeat(Chunker.MAX_LATIN_WORDS + 1))).isTrue();
        assertThat(Chunker.isTooLong("word ".repeat(10))).isFalse();
    }
}
