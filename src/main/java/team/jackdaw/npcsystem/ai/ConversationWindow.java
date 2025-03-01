package team.jackdaw.npcsystem.ai;

import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.api.Ollama;
import team.jackdaw.npcsystem.api.json.*;
import team.jackdaw.npcsystem.function.FunctionManager;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ConversationWindow {
    protected final UUID uuid;
    protected List<Message> messages;
    protected long updateTime = 0L;
    protected UUID target;
    private boolean onWait = false;

    public ConversationWindow(UUID uuid) {
        this.uuid = uuid;
        messages = Ollama.messageBuilder()
                .addMessage(Role.SYSTEM, AgentManager.getInstance().get(uuid).getInstruction())
                .build();
    }

    public Agent getAgent() {
        return AgentManager.getInstance().get(uuid);
    }

    public UUID getTarget() {
        return target;
    }

    public void setTarget(UUID target) {
        this.target = target;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public String getLastMessage() {
        return messages.get(messages.size() - 1).content;
    }

    public List<Tool> getTools() {
        return getAgent().getTools()
                .stream()
                .map(FunctionManager.getInstance()::getTools)
                .toList();
    }

    public boolean isOnWait() {
        return onWait;
    }

    public void onWait() {
        onWait = true;
    }

    public void offWait() {
        onWait = false;
    }

    /**
     * Say a message to the agent in this conversation
     *
     * @return The response
     */
    public ChatResponse chat(String message) {
        updateTime = System.currentTimeMillis();
        messages = Ollama.messageBuilder(messages)
                .addMessage(Role.USER, message)
                .build();
        ChatResponse response;
        try {
            response = Ollama.chat(messages, getTools());
            if (response.message.tool_calls != null) {
                MessageBuilder messagedBuilder = Ollama.messageBuilder(messages);
                for (ChatResponse.Message.ToolCall toolCall : response.message.tool_calls) {
                    String functionResult = FunctionManager.getInstance()
                            .callFunction(this, toolCall.function.name, toolCall.function.arguments)
                            .toString();
                    messagedBuilder.addToolMessage(toolCall.function.name, functionResult);
                }
                List<Message> messages2 = messagedBuilder.build();
                response = Ollama.chat(messages2, null);
            }
            messages = Ollama.messageBuilder(messages)
                    .addMessage(Role.ASSISTANT, response.message.content)
                    .build();
        } catch (Exception e) {
            NPCSystem.LOGGER.error("Failed to chat: " + e.getMessage());
            messages = Ollama.messageBuilder(messages)
                    .addMessage(Role.ASSISTANT, "I'm sorry, I can't do that.")
                    .build();
            response = null;
        }

        return response;
    }

    /**
     * Let Agent start the conversation
     * @return The response
     */
    public ChatResponse chat() {
        updateTime = System.currentTimeMillis();
        ChatResponse response;
        try {
            response = Ollama.chat(messages, null);
            messages = Ollama.messageBuilder(messages)
                    .addMessage(Role.ASSISTANT, response.message.content)
                    .build();
        } catch (Exception e) {
            NPCSystem.LOGGER.error("Failed to chat: " + e.getMessage());
            messages = Ollama.messageBuilder(messages)
                    .addMessage(Role.ASSISTANT, "I'm sorry, I can't do that.")
                    .build();
            response = null;
        }
        return response;
    }

    /**
     * Broadcast the last message by agent
     */
    public void broadcastMessage() {
        if (Objects.equals(getLastMessage(), "")) return;
        getAgent().broadcastMessage(getLastMessage());
    }

    public long getUpdateTime() {
        return updateTime;
    }


    protected boolean discard() {
        return true;
    }
}
