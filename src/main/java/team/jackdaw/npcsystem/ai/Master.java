package team.jackdaw.npcsystem.ai;

import team.jackdaw.npcsystem.ai.master.MasterCW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Master implements Agent {
    private static final Master master = new Master();
    private final List<String> tools = new ArrayList<>();
    private final UUID uuid = UUID.randomUUID();

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
        if (ConversationManager.isConversing(uuid)) return ConversationManager.getConversation(uuid);
        return new MasterCW();
    }

    @Override
    public void addTool(String tool) {
        tools.add(tool);
    }

    @Override
    public List<String> getTools() {
        return tools;
    }
}
