package com.chat.service.llm;

import com.chat.service.exception.ChatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// The only class that knows Spring AI's chat API
@Component
public class OllamaLlmClient implements LlmClient{

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmClient.class);

    private final ChatClient chatClient;

    public OllamaLlmClient(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String generate(String systemRules, String userMessage) {
        try {
            // "system" sets the rules. "user" carries the question and the code excerpts.
            String answer = chatClient.prompt()
                    .system(systemRules)
                    .user(userMessage)
                    .call()
                    .content();
            return answer == null ? "" : answer;
        }
        catch (RuntimeException ex) {
            // Ollama is stopped, the model is not pulled, or the call failed.
            log.warn("Chat model call failed: {}", ex.toString());
            throw new ChatException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The chat model failed or is not reachable. Check that Ollama is running "
                            + "and that qwen2.5-coder:1.5b is pulled.");
        }
    }

}
