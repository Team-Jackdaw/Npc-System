package team.jackdaw.npcsystem.api;

import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.api.json.*;

import java.util.List;

public interface Ollama {
    /**
     * Request completion from the Ollama API. Please update the API_URL, CHAT_MODEL before you use the API.
     * @param prompt The prompt to be completed.
     * @return CompletionResponse
     * @throws Exception If the request fails.
     */
    static CompletionResponse completion(String prompt) throws Exception {
        return Completion.completionRequest(Config.apiURL, Config.chat_model, prompt);
    }

    /**
     * Request completion from the Ollama API. Please update the API_URL, CHAT_MODEL before you use the API.
     * @param request The completion request.
     * @return CompletionResponse
     * @throws Exception If the request fails.
     */
    static CompletionResponse completion(CompletionRequest request) throws Exception {
        return Completion.completionRequest(Config.apiURL, request);
    }

    /**
     * Request completion from the Ollama API. Please update the API_URL, CHAT_MODEL before you use the API.
     * @param messages The messages to be sent to the chat model. (use the messageBuilder to create messages)
     * @param tools The tools (functions) that can be used in the chat model.
     * @return ChatResponse
     * @throws Exception If the request fails.
     */
    static ChatResponse chat(List<Message> messages, List<Tool> tools) throws Exception {
        return ChatCompletion.chatRequest(Config.apiURL, Config.chat_model, messages, tools);
    }

    /**
     * Create a message builder to create messages for the chat API.
     * @return ChatCompletion.MessageBuilder
     */
    static MessageBuilder messageBuilder() {
        return ChatCompletion.messageBuilder();
    }

    /**
     * Create a message builder from exist messages.
     * @param messages The messages to be added to the builder.
     * @return ChatCompletion.MessageBuilder
     */
    static MessageBuilder messageBuilder(List<Message> messages) {
        return ChatCompletion.messageBuilder(messages);
    }
}
