package com.chat.service.answer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Checks the citation markers in the model's answer, such as [C1].
public final class CitationValidator {

    // Matches a label like [C1], [C2], and so on
    private static final Pattern MARKER = Pattern.compile("\\[(C\\d+)\\]");

    // The cleaned answer text, and the labels that were used, in the order they first appeared
    public record Result(
            String text,
            List<String> usedLabels
    ) {
    }

    private CitationValidator() {
    }

// ------ AllowedLabels: the labels we gave the model, for example [C1, C2, C3] ------
    public static Result validate(String answer, List<String> allowedLabels) {
        Set<String> allowed = new HashSet<>(allowedLabels);
        Set<String> used = new LinkedHashSet<>();

        Matcher matcher = MARKER.matcher(answer);
        StringBuilder cleaned = new StringBuilder();

        while (matcher.find()) {
            String label = matcher.group(1);
            if (allowed.contains(label)) {
                // A real label: keep the marker and remember that it was used.
                used.add(label);
                matcher.appendReplacement(cleaned, Matcher.quoteReplacement(matcher.group()));
            }
            else {
                // An invented label: remove it from the text.
                matcher.appendReplacement(cleaned, "");
            }
        }
        matcher.appendTail(cleaned);

        return new Result(cleaned.toString().trim(), new ArrayList<>(used));
    }
}
