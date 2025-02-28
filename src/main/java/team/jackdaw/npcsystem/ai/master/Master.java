package team.jackdaw.npcsystem.ai.master;

import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;

import java.util.UUID;

public class Master extends Agent {
    private static final Master master;

    static {
        master = new Master();
        AgentManager.getInstance().register(master);
    }

    private Master() {
        this.uuid = UUID.randomUUID();
    }

    /**
     * Get the unique master agent
     *
     * @return the master agent
     */
    public static Master getMaster() {
        return master;
    }

    @Override
    public String getInstruction() {
        return "Your are the master of this Minecraf world!";
    }

    @Override
    protected ConversationWindow createConversationWindows() {
        return new MasterCW();
    }

    @Override
    public void broadcastMessage(String message) {
    }
}
