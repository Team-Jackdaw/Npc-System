package team.jackdaw.npcsystem.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.jackdaw.npcsystem.api.chatcompletion.ChatCompletion;
import team.jackdaw.npcsystem.api.chatcompletion.json.ChatResponse;
import team.jackdaw.npcsystem.api.chatcompletion.json.Message;
import team.jackdaw.npcsystem.api.completion.Completion;
import team.jackdaw.npcsystem.api.completion.json.CompletionResponse;
import team.jackdaw.npcsystem.api.embedding.Embedding;
import team.jackdaw.npcsystem.api.embedding.json.EmbeddingResponse;
import team.jackdaw.npcsystem.api.json.Tool;

import java.util.List;

public class Ollama {
    public static String API_URL = "http://localhost:11434";
    public static String CHAT_MODEL = "mistral";
    public static String EMBEDDING_MODEL = "nomic-embed-text";
    public static CompletionResponse completion(@NotNull String prompt) throws Exception {
        return Completion.completionRequest(API_URL, CHAT_MODEL, prompt);
    }
    public static ChatResponse chat(@NotNull List<Message> messages, @Nullable List<Tool> tools) throws Exception {
        return ChatCompletion.chatRequest(API_URL, CHAT_MODEL, messages, tools);
    }
    @Contract(" -> new")
    public static ChatCompletion.MessageBuilder messageBuilder() {
        return ChatCompletion.messageBuilder();
    }
    public static EmbeddingResponse embed(@NotNull List<String> input) throws Exception {
        return Embedding.embedRequest(API_URL, EMBEDDING_MODEL, input);
    }
}
