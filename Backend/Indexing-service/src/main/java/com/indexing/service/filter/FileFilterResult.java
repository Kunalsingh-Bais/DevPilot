package com.indexing.service.filter;

// The answer for one file path: keep it, or skip it and say why.
public record FileFilterResult(
        boolean keep,
        String reason
) {

    // Factory method: a clear way to say "keep this file".
    public static FileFilterResult Keep() {
        return new FileFilterResult(true, "");
    }

    // Factory method: a clear way to say "skip this file, because of this reason".
    public static FileFilterResult skip(String reason) {
        return new FileFilterResult(false, reason);
    }
}
