package com.knowledgeapp.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.knowledgeapp.api.ApiException;
import com.knowledgeapp.api.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KnowledgePathsTest {

    @TempDir
    Path temp;

    @ParameterizedTest
    @ValueSource(strings = {
        "../secret.md", "a/../../secret.md", "..\\secret.md", "a\\..\\..\\x.md",
        "/etc/passwd.md", "\\\\server\\share\\x.md", "C:/Windows/x.md", "c:x.md",
        "notes.txt", "", "   ", "a/b/..", "file:x.md"
    })
    void rejectsUnsafePaths(String path) {
        assertThatThrownBy(() -> KnowledgePaths.normalizeRelative(path))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.INVALID_PATH);
    }

    @Test
    void normalizesSafePaths() {
        assertThat(KnowledgePaths.normalizeRelative("./a//b/./c.md")).isEqualTo("a/b/c.md");
        assertThat(KnowledgePaths.normalizeRelative("folder\\doc.md")).isEqualTo("folder/doc.md");
    }

    @Test
    void resolvesInsideRootAndReportsMissing() throws IOException {
        Path root = Files.createDirectories(temp.resolve("knowledge"));
        Files.writeString(root.resolve("a.md"), "# A");

        assertThat(KnowledgePaths.resolveExisting(root, "a.md")).isEqualTo(root.resolve("a.md").toRealPath());
        assertThatThrownBy(() -> KnowledgePaths.resolveExisting(root, "missing.md"))
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.DOC_NOT_FOUND);
    }

    @Test
    void rejectsSymlinkEscape() throws IOException {
        Path root = Files.createDirectories(temp.resolve("knowledge"));
        Path outside = Files.writeString(temp.resolve("outside.md"), "# secret");
        try {
            Files.createSymbolicLink(root.resolve("link.md"), outside);
        } catch (IOException | UnsupportedOperationException ex) {
            assumeTrue(false, "symbolic links not permitted on this machine");
        }
        assertThatThrownBy(() -> KnowledgePaths.resolveExisting(root, "link.md"))
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.INVALID_PATH);
    }
}
