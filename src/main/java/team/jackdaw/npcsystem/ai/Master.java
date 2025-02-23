package team.jackdaw.npcsystem.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Master implements Agent {
    private static final Master master = new Master();
    private final List<String> tools = new ArrayList<>();
    private final UUID uuid = UUID.randomUUID();

    static {
        AgentManager.registerAgent(getMaster());
    }

    private Master() {

    }

    /**
     * Get the unique master agent
     * @return the master agent
     */
    public static Master getMaster() {
        return master;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public ConversationWindow getConversationWindows() {
        ConversationManager.startConversation(this);
        return ConversationManager.getConversation(this.uuid);
    }

    @Override
    public void addTool(String tool) {
        tools.add(tool);
    }

    @Override
    public List<String> getTools() {
        return tools;
    }

    @Override
    public boolean discard() {
        return false;
    }
}
