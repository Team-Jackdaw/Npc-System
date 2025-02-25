package team.jackdaw.npcsystem.ai;

import team.jackdaw.npcsystem.api.json.ChatResponse;
import team.jackdaw.npcsystem.api.json.Message;
import team.jackdaw.npcsystem.api.json.Tool;

import java.util.List;

public interface ConversationWindow {
    /**
     * Get the agent of the conversation
     * @return The agent
     */
    Agent getAgent();

    /**
     * Get the conversation history of this conversation
     * @return The messages
     */
    List<Message> getMessages();

    /**
     * Get the tools that available for this conversation
     * @return The tools
     */
    List<Tool> getTools();

    /**
     * Say a message to the agent in this conversation
     * @return The response
     */
    ChatResponse chat(String message);

    /**
     * Get the time of the last update
     * @return The time of the last update
     */
    long getUpdateTime();

    /**
     * Discard the conversation
     */
    boolean discard();
}
