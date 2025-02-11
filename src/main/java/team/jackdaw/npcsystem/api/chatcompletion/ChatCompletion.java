package team.jackdaw.npcsystem.api.chatcompletion;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.jackdaw.npcsystem.api.Header;
import team.jackdaw.npcsystem.api.Request;
import team.jackdaw.npcsystem.api.chatcompletion.json.*;
import team.jackdaw.npcsystem.api.json.Tool;

import java.util.ArrayList;
import java.util.List;

public class ChatCompletion {

    /**
     * Create or modify a chat completion for the NPC from the OpenAI API
     * @param url The url to send the request to. It should look like "<a href="http://localhost:11434">http://localhost:11434</a>"
     * @param model The model to use
     * @param messages The messages to use
     * @param tools The tools to use (optional)
     * @return The chat completion response
     * @throws Exception If the chat completion is not created or modified successfully
     */
    public static @NotNull ChatResponse chatRequest(@NotNull String url, @NotNull String model, @NotNull List<Message> messages, @Nullable List<Tool> tools) throws Exception {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.model = model;
        chatRequest.messages = messages;
        chatRequest.stream = false;
        if (tools != null && !tools.isEmpty()) {
            chatRequest.tools = tools;
        }
        String res = Request.sendRequest(chatRequest.toJson(), url + "/api/chat", Header.buildDefault(), Request.Action.POST);
        return ChatResponse.fromJson(res);
    }

    @Contract(" -> new")
    public static @NotNull MessageBuilder messageBuilder() {
        return new MessageBuilder();
    }

    public static @NotNull MessageBuilder messageBuilder(@NotNull List<Message> messages) {
        MessageBuilder builder = new MessageBuilder();
        for (Message message : messages) {
            builder.addMessage(Role.valueOf(message.role.toUpperCase()), message.content);
        }
        return builder;
    }

    public static class MessageBuilder {
        private final List<Message> messages = new ArrayList<>();

        public MessageBuilder addMessage(@NotNull Role role, @NotNull String content) {
            Message message = new Message();
            message.role = role.name().toLowerCase();
            message.content = content;
            messages.add(message);
            return this;
        }

        public MessageBuilder addToolMessage(@NotNull String name, @NotNull String content) {
            Message message = ToolMessage.newToolMessage(name, content);
            messages.add(message);
            return this;
        }

        public @NotNull List<Message> build() {
            return messages;
        }
    }
}

