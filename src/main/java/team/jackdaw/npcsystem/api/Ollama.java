package team.jackdaw.npcsystem.api;

import team.jackdaw.npcsystem.api.chatcompletion.ChatCompletion;
import team.jackdaw.npcsystem.api.chatcompletion.json.ChatResponse;
import team.jackdaw.npcsystem.api.chatcompletion.json.Message;
import team.jackdaw.npcsystem.api.completion.Completion;
import team.jackdaw.npcsystem.api.completion.json.CompletionRequest;
import team.jackdaw.npcsystem.api.completion.json.CompletionResponse;
import team.jackdaw.npcsystem.api.embedding.Embedding;
import team.jackdaw.npcsystem.api.embedding.json.EmbeddingRequest;
import team.jackdaw.npcsystem.api.embedding.json.EmbeddingResponse;
import team.jackdaw.npcsystem.api.json.Tool;

import java.util.List;

public class Ollama {
    public static String API_URL = "http://localhost:11434";
    public static String CHAT_MODEL = "mistral";
    public static String EMBEDDING_MODEL = "nomic-embed-text";

    /**
     * Request completion from the Ollama API. Please update the API_URL, CHAT_MODEL before you use the API.
     * @param prompt The prompt to be completed.
     * @return CompletionResponse
     * @throws Exception If the request fails.
     */
    public static CompletionResponse completion(String prompt) throws Exception {
        return Completion.completionRequest(API_URL, CHAT_MODEL, prompt);
    }

    /**
     * Request completion from the Ollama API. Please update the API_URL, CHAT_MODEL before you use the API.
     * @param request The completion request.
     * @return CompletionResponse
     * @throws Exception If the request fails.
     */
    public static CompletionResponse completion(CompletionRequest request) throws Exception {
        return Completion.completionRequest(API_URL, request);
    }

    /**
     * Request completion from the Ollama API. Please update the API_URL, CHAT_MODEL before you use the API.
     * @param messages The messages to be sent to the chat model. (use the messageBuilder to create messages)
     * @param tools The tools (functions) that can be used in the chat model.
     * @return ChatResponse
     * @throws Exception If the request fails.
     */
    public static ChatResponse chat(List<Message> messages, List<Tool> tools) throws Exception {
        return ChatCompletion.chatRequest(API_URL, CHAT_MODEL, messages, tools);
    }

    /**
     * Create a message builder to create messages for the chat API.
     * @return ChatCompletion.MessageBuilder
     */
    public static ChatCompletion.MessageBuilder messageBuilder() {
        return ChatCompletion.messageBuilder();
    }

    /**
     * Create a message builder from exist messages.
     * @param messages The messages to be added to the builder.
     * @return ChatCompletion.MessageBuilder
     */
    public static ChatCompletion.MessageBuilder messageBuilder(List<Message> messages) {
        return ChatCompletion.messageBuilder(messages);
    }

    /**
     * Embed the input texts to vectors. Please update the API_URL and EMBEDDING_MODEL before you use the API.
     * @param input The input to be embedded.
     * @return EmbeddingResponse
     * @throws Exception If the request fails.
     */
    public static EmbeddingResponse embed(List<String> input) throws Exception {
        return Embedding.embedRequest(API_URL, EMBEDDING_MODEL, input);
    }

    /**
     * Embed the input texts to vectors. Please update the API_URL and EMBEDDING_MODEL before you use the API.
     * @param request The embedding request.
     * @return EmbeddingResponse
     * @throws Exception If the request fails.
     */
    public static EmbeddingResponse embed(EmbeddingRequest request) throws Exception {
        return Embedding.embedRequest(API_URL, request);
    }
}
