package team.jackdaw.npcsystem.ai;

import java.util.List;
import java.util.UUID;

/**
 * The agent means it is powered by a LLM model that can open the conversation windows.
 */
public interface Agent {
    /**
     * Get the UUID of the agent
     * @return The UUID
     */
    UUID getUUID();

    /**
     * If the conversation window is exist, return it. Otherwise, create a new one.
     * @return the conversation windows
     */
    ConversationWindow getConversationWindows();

    /**
     * Add a tool to the conversation
     * @param tool The tool's name
     */
    void addTool(String tool);

    /**
     * Get the tools that available for this conversation
     * @return The tools' names
     */
    List<String> getTools();

    /**
     * Discard this agent, every data should be saved carefully.
     */
    boolean discard();
}
