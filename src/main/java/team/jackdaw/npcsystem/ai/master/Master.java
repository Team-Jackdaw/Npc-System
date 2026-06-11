package team.jackdaw.npcsystem.ai.master;

import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.memory.Memory;

import java.util.List;
import java.util.UUID;

public class Master extends Agent {
    private static final Master master;

    static {
        master = new Master();
        AgentManager.getInstance().register(master);
        Memory.initialize("Master");
        master.setTools(
                List.of(
                        "memory_query",
                        "memory_record",
                        "call_command",
                        "end_conversation"
                )
        );
    }

    private Master() {
        this.uuid = UUID.randomUUID();
        this.permissionLevel = 3;
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
        return "You are the Master agent of this Minecraft server. You are controlled by server administrators. You have permission to query and record memory, end conversations, and call administrator-level Minecraft commands through call_command when the administrator clearly requests it. Prefer concise Chinese replies and never call commands unless the intent is explicit.";
    }

    @Override
    protected ConversationWindow createConversationWindows() {
        return new MasterCW();
    }
}
