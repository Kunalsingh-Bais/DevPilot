package com.chat.service.llm;

// The contract for "ask the chat model to write an answer".
// AnswerService depends on this interface, not on Ollama.
public interface LlmClient {

    String generate(
            String systemRules,   // instructions that control how the AI should respond
            String userMessage);  // the question plus the labeled code excerpts
}
