package com.indexing.service.filter;

import java.util.Locale;
import java.util.Set;

public final class FileFilter {

    static final long MAX_FILE_BYTES = 200L * 1024; // 200KB

    // Folders that hold dependencies, generated output, or IDE settings are not allowed.
    private static final Set<String> EXCLUDED_FOLDERS = Set.of("node_modules", "target",
            "build", "dist", "out", "bin", "vendor", ".git", ".idea", ".vscode", ".angular",
            ".gradle", ".next", "coverage", "__pycache__");

    // The only file extensions we index. Anything not listed is skipped.
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("java", "kt", "js", "jsx",
            "ts", "tsx", "py", "go", "rs", "rb", "php", "cs", "c", "cpp", "h", "hpp", "sql",
            "html", "css", "scss", "xml", "yml", "yaml", "properties", "gradle", "toml",
            "json", "sh", "md", "txt");

    // Lock files are large, generated, and rarely useful when answering questions.
    private static final Set<String> EXCLUDED_FILE_NAMES = Set.of("package-lock.json",
            "yarn.lock", "pnpm-lock.yaml", "composer.lock", "Pipfile.lock");

    // Constructor
    private FileFilter() {
    }

// ------ Returns "keep" or "skip with reason" for one file path and its size in bytes ------
    public static FileFilterResult evaluate(String path, long sizeBytes) {
        if (path == null || path.isBlank()) {
            return FileFilterResult.skip("empty path");
        }

        String[] parts = path.split("/");

        // Refuse paths that try to leave the repository, such as "../secret".
        for (String part : parts) {
            if (part.equals("..") || part.equals(".")) {
                return FileFilterResult.skip("path tries to leave the repository");
            }
        }

        String fileName = parts[parts.length - 1];

        // Check every folder on the path, not only the first one.
        for (int i=0; i<parts.length-1; i++) {
            if (EXCLUDED_FOLDERS.contains(parts[i].toLowerCase(Locale.ROOT))) {
                return FileFilterResult.skip("inside excluded folder: " + parts[i]);
            }
        }

        if (EXCLUDED_FILE_NAMES.contains(fileName)) {
            return FileFilterResult.skip("lock file");
        }

        if (isLikelySecret(fileName)) {
            // Secrets are never sent to an AI provider, even when they are inside the repository.
            return FileFilterResult.skip("may contain secrets");
        }

        // Find the last dot. A dot at position 0 means a hidden file such as ".gitignore".
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return FileFilterResult.skip("no file extension");
        }

        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return FileFilterResult.skip("file type not indexed: ." + extension);
        }

        if (sizeBytes <= 0) {
            return FileFilterResult.skip("empty file");
        }

        if (sizeBytes > MAX_FILE_BYTES) {
            return FileFilterResult.skip("file larger than " + (MAX_FILE_BYTES / 1024) + " KB");
        }

        return FileFilterResult.Keep();
    }

// ------ Names that often hold passwords, keys, or tokens ------
    private static boolean isLikelySecret(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.startsWith(".env") || lower.endsWith(".pem") || lower.endsWith(".key")
                || lower.endsWith(".p12") || lower.endsWith(".jks") || lower.equals("id_rsa") || lower.equals("credentials");
    }
}
