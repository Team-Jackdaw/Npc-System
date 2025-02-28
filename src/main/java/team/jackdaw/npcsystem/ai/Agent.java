package team.jackdaw.npcsystem.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The agent means it is powered by a LLM model that can open the conversation windows.
 */
public abstract class Agent {
    protected UUID uuid;
    protected final List<String> tools = new ArrayList<>();

    /**
     * Get the UUID of the agent
     * @return The UUID
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Get the instruction of the agent
     * @return The instruction
     */
    public abstract String getInstruction();

    public ConversationWindow getNewConversationWindows() {
        if (ConversationManager.getInstance().isRegistered(uuid)) {
            ConversationManager.getInstance().remove(uuid);
        }
        ConversationManager.getInstance().register(this);
        return ConversationManager.getInstance().get(uuid);
    }

    /**
     * If the conversation window is exist, return it. Otherwise, register a new one.
     * @return the conversation windows
     */
    public ConversationWindow getConversationWindows() {
        if (!ConversationManager.getInstance().isRegistered(uuid)) {
            ConversationManager.getInstance().register(this);
        }
        return ConversationManager.getInstance().get(uuid);
    }

    /**
     * Create a new conversation window
     * @return the conversation windows
     */
    protected abstract ConversationWindow createConversationWindows();

    public abstract void broadcastMessage(String message);

    /**
     * Add a tool to the agent
     * @param tools The tools' names
     */
    public void setTools(List<String> tools) {
        this.tools.clear();
        this.tools.addAll(tools);
    }

    /**
     * Add a tool to the conversation
     * @param tool The tool's name
     */
    public void addTool(String tool) {
        tools.add(tool);
    }

    /**
     * Get the tools that available for this conversation
     * @return The tools' names
     */
    public List<String> getTools() {
        return tools;
    }

    /**
     * Discard this agent, every data should be saved carefully.
     */
    public boolean discard() {
        if (ConversationManager.getInstance().isRegistered(uuid)) {
            return ConversationManager.getInstance().discard(uuid);
        }
        return true;
    }
}
