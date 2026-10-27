package com.repository.service.service;

import com.repository.service.client.GithubBlob;
import com.repository.service.client.GithubClient;
import com.repository.service.dto.RepositoryFileContent;
import com.repository.service.exception.GithubApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class FileContentService {

    private static final long MAX_FILE_BYTES = 200L * 1024;   // 200 KB
    private static final int BINARY_CHECK_BYTES = 8000;

    private final GithubClient githubClient;

    // Constructor
    public FileContentService(GithubClient githubClient) {
        this.githubClient = githubClient;
    }

// ------ Method 1: Returns the decoded text of one file, identified by its blob SHA. ------
    public RepositoryFileContent getFileContent(String githubOwner, String repoName, String blobSha) {
        // Validate every value that goes into the GitHub URL.
        GithubInputRules.requireOwner(githubOwner);
        GithubInputRules.requireRepositoryName(repoName);
        GithubInputRules.requireBlobSha(blobSha);

        GithubBlob blob = githubClient.getBlob(githubOwner, repoName, blobSha);

        // Check the size before decoding
        if (blob.size() > MAX_FILE_BYTES) {
            throw new GithubApiException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "File is larger than " + (MAX_FILE_BYTES/ 1024) + " KB and was skipped.");
        }

        if (blob.content() == null || !"base64".equals(blob.encoding())) {
            throw new GithubApiException(HttpStatus.BAD_GATEWAY,
                    "GitHub returned file content in an unexpected format.");
        }

        byte[] bytes = decodeBase64(blob.content());

        if (looksBinary(bytes)) {
            throw new GithubApiException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "File is binary and cannot be indexed as text.");
        }

        return new RepositoryFileContent(blob.sha(), bytes.length, decodeUtf8(bytes));
    }

// ------ HELPER METHODS ------

    // Decodes base64 text.
     private byte[] decodeBase64(String encoded) {
        try {
            return Base64.getMimeDecoder().decode(encoded);
        } catch (IllegalArgumentException ex) {
            throw new GithubApiException(HttpStatus.BAD_GATEWAY,
                    "GitHub sent file content that could not be decoded.");
        }
     }

     private boolean looksBinary(byte[] bytes) {
        int limit = Math.min(bytes.length, BINARY_CHECK_BYTES);
        for (int i = 0; i < limit; i++) {
            if (bytes[i] == 0) {
                return true;
            }
        }
        return false;
     }

     // Converts bytes to text.
     private String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        }
        catch (CharacterCodingException ex) {
            throw new GithubApiException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "File is not valid UTF-8 text and was skipped.");
        }
     }
}
