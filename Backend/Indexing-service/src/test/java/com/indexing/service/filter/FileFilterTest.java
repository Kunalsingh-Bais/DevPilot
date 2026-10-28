package com.indexing.service.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class
FileFilterTest {

    // A typical small file size, used when size doesn't matter for the test.
    private static final long SMALL =  1_000;

    @Test
    void keepsJavaSourceFile() {
        assertThat(FileFilter.evaluate("src/main/java/App.java", SMALL).keep()).isTrue();
    }

    @Test
    void keepsMarkdownReadme() {
        assertThat(FileFilter.evaluate("README.md", SMALL).keep()).isTrue();
    }

    @Test
    void skipsFilesInsideNodeModulesAtAnyDepth() {
        assertThat(FileFilter.evaluate("frontend/node_modules/lodash/index.js", SMALL).keep()).isFalse();
    }

    @Test
    void skipsFilesInsideTargetFolder() {
        assertThat(FileFilter.evaluate("target/classes/App.class", SMALL).keep()).isFalse();
    }

    @Test
    void skipsImages() {
        // Images are not text, so they can't be embedded or cited as code.
        assertThat(FileFilter.evaluate("docs/logo.png", SMALL).keep()).isFalse();
    }

    @Test
    void skipsLockFiles() {
        assertThat(FileFilter.evaluate("frontend/package-lock.json", SMALL).keep()).isFalse();
    }

    @Test
    void skipsEnvAndKeyFiles() {
        // These can contain passwords or private keys, so they must never be indexed.
        assertThat(FileFilter.evaluate(".env", SMALL).keep()).isFalse();
        assertThat(FileFilter.evaluate("config/.env.production", SMALL).keep()).isFalse();
        assertThat(FileFilter.evaluate("certs.server.key", SMALL).keep()).isFalse();
    }

    @Test
    void skipsFilesLargerThanLimit() {
        // One byte over the limit must be skipped.
        assertThat(FileFilter.evaluate("src/Big.java", FileFilter.MAX_FILE_BYTES + 1).keep()).isFalse();
    }

    @Test
    void skipsEmptyFiles() {
        assertThat(FileFilter.evaluate("src/Empty.java", 0).keep()).isFalse();
    }

    @Test
    void skipsHiddenFilesWithoutExtension() {
        assertThat(FileFilter.evaluate(".gitignore", SMALL).keep()).isFalse();
    }

    @Test
    void skipsPathThatTryToLeaveTheRepository() {
        assertThat(FileFilter.evaluate("../../etc/passwd", SMALL).keep()).isFalse();
    }
}
