package com.chat.service.answer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Checks that each citation is supported by the sentence it is attached to.
// It compares meaningful words, so it confirms word overlap, not that the claim is true.
public final class CitationSupportChecker {

    // A citation marker such as [C1]
    private static final Pattern MARKER = Pattern.compile("\\[(C\\d+)\\]");

    // Splits text into sentence-like pieces ending in . ! ? or into a single terminator or newline.
    private static final Pattern PIECE = Pattern.compile("[^.!?\\n]+[.!?]?|[.!?\\n]");

    // Common words that match almost anything
    private static final Set<String> STOP_WORDS = Set.of(
            "this", "that", "with", "from", "there", "their", "which", "into", "uses",
            "have", "been", "your", "about", "when", "where", "what", "also", "will",
            "each", "only", "they", "them", "than", "does", "code", "file");

    // A sentence must share at least this many meaningful words with the cited chunk.
    static final int MIN_SHARED_WORDS = 2;

    // The cleaned answer, and the labels whose citations survived, in the order they first appeared.
    public record Result(String text, List<String> keptLabels) {
    }

    // constructor
    private CitationSupportChecker() {
    }

// ------ chunkTextByLabel: for each label we gave the model, its file path plus its code text ------
    public static Result check(String answer, Map<String, String> chunkTextByLabel) {
        StringBuilder out = new StringBuilder();
        Set<String> kept = new LinkedHashSet<>();

        Matcher piece = PIECE.matcher(answer);
        while (piece.find()) {
            String text = piece.group();

            // A piece without a citation is copied unchanged.
            if (!MARKER.matcher(text).find()) {
                out.append(text);
                continue;
            }

            // The meaningful words of the sentence, without the citation markers.
            Set<String> claimWords = words(MARKER.matcher(text).replaceAll(""));

            // Keep each marker only if its chunk supports this sentence. Remove the rest.
            Matcher marker = MARKER.matcher(text);
            StringBuilder cleaned = new StringBuilder();

            while (marker.find()) {
                String label = marker.group(1);

                if (supports(claimWords, chunkTextByLabel.get(label))) {
                    kept.add(label);
                    marker.appendReplacement(cleaned, Matcher.quoteReplacement(marker.group()));
                }
                else {
                    marker.appendReplacement(cleaned, "");
                }
            }
            marker.appendTail(cleaned);

            // Removing a marker can leave a space before the full stop
            out.append(cleaned.toString().replaceAll("\\s+([.!?,;])", "$1"));
        }

        return new Result(out.toString().trim(), new ArrayList<>(kept));
    }

// ------ True when the sentence and the chunk share enough meaningful words ------
    private static boolean supports(Set<String> claimWords, String chunkText) {
        if (chunkText == null) {
            return false;
        }

        Set<String> chunkWords = words(chunkText);
        long shared = claimWords.stream().filter(chunkWords::contains).count();

        return shared >= MIN_SHARED_WORDS;
    }

// ------ Lowercase words of at least 4 letters, without stop words ------
    static Set<String> words(String text) {
        String spaced = text.replaceAll("([a-z])([A-Z])", "$1 $2");

        return Arrays.stream(spaced.toLowerCase(Locale.ROOT).split("[^a-z]+"))
                .filter(w -> w.length() >= 4 && !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }


}
