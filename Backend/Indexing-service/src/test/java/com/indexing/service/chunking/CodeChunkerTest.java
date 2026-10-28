package com.indexing.service.chunking;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class CodeChunkerTest {

    private String numberedLines(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> "line " + i)
                .collect(Collectors.joining("\n", "", "\n"));
    }

    @Test
    void emptyFilesGivesNoChunks() {
        assertThat(CodeChunker.split("")).isEmpty();
    }

    @Test
    void shortFileIsOneChunkWithCorrectLineNumbers() {
        List<TextChunk> chunks = CodeChunker.split(numberedLines(3));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).startLine()).isEqualTo(1);
        assertThat(chunks.get(0).endLine()).isEqualTo(3);
        // The trailing newline is not counted as an extra line.
        assertThat(chunks.get(0).content()).isEqualTo("line 1\nline 2\nline 3");
    }

    @Test
    void windowsLineEndingsAreNormalized() {
        List<TextChunk> chunks = CodeChunker.split("a\r\nb\r\n");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).content()).isEqualTo("a\nb");
    }

    @Test
    void longFilesIsSplitWithoutGapsOrOverlap() {
        List<TextChunk> chunks = CodeChunker.split(numberedLines(500));

        assertThat(chunks.size()).isGreaterThan(1);
        // The first chunk starts at line 1, and the last chunk ends at line 500.
        assertThat(chunks.get(0).startLine()).isEqualTo(1);
        assertThat(chunks.get(chunks.size() -1).endLine()).isEqualTo(500);

        // Each chunk starts exactly one line after the previous chunk ended.
        for (int i=1; i<chunks.size(); i++) {
            assertThat(chunks.get(i).startLine()).isEqualTo(chunks.get(i-1).endLine() + 1);
        }
    }

    @Test
    void linesNumbersMatchTheActualText() {
        // In "line N" files, the first and last lines of a chunk must match its line numbers.
        List<TextChunk> chunks = CodeChunker.split(numberedLines(500));

        for (TextChunk chunk: chunks) {
            String[] lines = chunk.content().split("\n");
            assertThat(lines[0]).isEqualTo("line " + chunk.startLine());
            assertThat(lines[lines.length -1]).isEqualTo("line " + chunk.endLine());
        }
    }

    @Test
    void chunksStayUnderTheCharacterLimit() {
        // 200-character lines: about 7 fit before the 1,500-character limit.
        String longLines = ("X".repeat(200) + "\n").repeat(50);

        for (TextChunk chunk: CodeChunker.split(longLines)) {
            assertThat(chunk.content().length()).isLessThanOrEqualTo(CodeChunker.MAX_CHARACTER);
        }
    }

    @Test
    void singleVeryLongLineBecomesItsOwnChunk() {
        // One line longer than the limit must still be kept. It must not cause an endless loop.
        List<TextChunk> chunks = CodeChunker.split("x".repeat(3000));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).startLine()).isEqualTo(1);
        assertThat(chunks.get(0).endLine()).isEqualTo(1);
    }


}
