package team.jackdaw.npcsystem.ai;

/**
 * The agent means it is powered by a LLM model that can open the conversation windows.
 */
public interface Agent {
    /**
     * Open the conversation windows.
     * @return the conversation windows
     */
    ConversationWindow getConversationWindows();
}
