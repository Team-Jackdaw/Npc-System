package team.jackdaw.npcsystem.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.jackdaw.npcsystem.api.json.ChatRequest;
import team.jackdaw.npcsystem.api.json.ChatResponse;
import team.jackdaw.npcsystem.api.json.Message;
import team.jackdaw.npcsystem.api.json.Tool;

import java.util.ArrayList;
import java.util.List;

public class ChatCompletion {

    public static @NotNull ChatResponse chatRequest(@NotNull String url, @NotNull String model, @NotNull List<Message> messages, @Nullable List<Tool> tools) throws Exception {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.model = model;
        chatRequest.messages = messages;
        chatRequest.stream = false;
        if (tools != null && !tools.isEmpty()) {
            chatRequest.tools = tools;
        }
        String res = Request.sendRequest(chatRequest.toJson(), url, Header.buildDefault(), Request.Action.POST);
        return ChatResponse.fromJson(res);
    }

    @Contract(" -> new")
    public static @NotNull MessageBuilder messageBuilder() {
        return new MessageBuilder();
    }

    public static class MessageBuilder {
        private final List<Message> messages = new ArrayList<>();

        public MessageBuilder addMessage(@NotNull String role, @NotNull String content) {
            Message message = new Message();
            message.role = role;
            message.content = content;
            messages.add(message);
            return this;
        }

        public @NotNull List<Message> build() {
            return messages;
        }
    }
}

