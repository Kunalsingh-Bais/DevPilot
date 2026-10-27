package com.indexing.service.chunking;

import java.util.ArrayList;
import java.util.List;

// Splits the text of one file into chunks made of whole lines.
// It is plain Java with no Spring.
public final class CodeChunker {

    // Limits for one chunk
    public static final int MAX_LINES = 80;
    public static final int MAX_CHARACTER = 1500;

    // Constructor
    private CodeChunker() {
    }

// ------ Returns chunk for one file ------
    public static List<TextChunk> split(String content) {
        List<String> lines = toLines(content);     // Convert file into lines
        List<TextChunk> chunks = new ArrayList<>();

        int start = 0;     // Index of the first line of this chunk (line 1 = index 0, ..)

        while (start < lines.size()) {
            int end = start;     // tells where the chunk currently ends
            int charCount = 0;   // tracks how many characters the current chunk contains

            while (end < lines.size() && end - start < MAX_LINES) {
                // Count the line plus one character for its newline.
                int lineCost = lines.get(end).length() + 1;

                // Stop before going over character limit.
                if (end > start && charCount + lineCost > MAX_CHARACTER) {
                    break;
                }

                charCount += lineCost;
                end++;          // index line
            }

            // Join the lines bac together
            String text = String.join("\n", lines.subList(start, end));

            // Convert to 1-based numbers: Java index 0 is line 1.
            chunks.add(new TextChunk(start + 1, end, text));

            // The next chunk starts right after this one.
            start = end;
        }
        return chunks;
    }

// ------ Splits text into lines and remove the empty piece ------
    private static List<String> toLines(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>(List.of(content.split("\r?\n", -1)));

        // A file that ends with a newline gives an empty last piece.
        if (lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        return lines;
    }
}