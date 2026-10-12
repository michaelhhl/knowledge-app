package com.knowledgeapp.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownParserTest {

    @Test
    void parsesFrontMatter() {
        KnowledgeDocument doc = MarkdownParser.parse("a/onboarding.md", """
                ---
                id: kb-onboarding
                title: 入职检查清单
                tags: [hr, onboarding]
                updated: 2026-10-04
                status: published
                ---

                # 入职检查清单

                ## 第一天
                开权限。
                """);

        assertThat(doc.id()).isEqualTo("kb-onboarding");
        assertThat(doc.title()).isEqualTo("入职检查清单");
        assertThat(doc.tags()).isEqualTo(List.of("hr", "onboarding"));
        assertThat(doc.updated()).isEqualTo("2026-10-04");
        assertThat(doc.published()).isTrue();
        assertThat(doc.body()).doesNotContain("status:").startsWith("\n# 入职检查清单");
    }

    @Test
    void fallsBackToH1ThenFileName() {
        assertThat(MarkdownParser.parse("x.md", "# 标题一\n正文").title()).isEqualTo("标题一");
        assertThat(MarkdownParser.parse("dir/some-file.md", "正文").title()).isEqualTo("some-file");
        assertThat(MarkdownParser.parse("dir/some-file.md", "正文").id()).isEqualTo("kb-some-file");
    }

    @Test
    void defaultsToPublishedAndRecognizesDraft() {
        assertThat(MarkdownParser.parse("a.md", "# A").published()).isTrue();
        KnowledgeDocument draft = MarkdownParser.parse("b.md", "---\ntitle: B\nstatus: Draft\n---\n# B\n");
        assertThat(draft.published()).isFalse();
        assertThat(draft.status()).isEqualTo("draft");
    }

    @Test
    void handlesCrlfAndBom() {
        KnowledgeDocument doc = MarkdownParser.parse("a.md", "\uFEFF---\r\ntitle: T\r\n---\r\n## S\r\nx\r\n");
        assertThat(doc.title()).isEqualTo("T");
        assertThat(doc.body()).isEqualTo("## S\nx\n");
    }
}
