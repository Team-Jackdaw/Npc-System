package team.jackdaw.npcsystem.function;

import net.minecraft.server.level.ServerPlayer;
import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.task.NpcTask;

import java.util.Map;
import java.util.Optional;

abstract class NpcTaskFunction extends CustomFunction {
    protected Optional<NPCEntity> currentNpc(ConversationWindow conversation) {
        if (conversation == null) {
            return Optional.empty();
        }
        Agent agent = conversation.getAgent();
        if (!(agent instanceof NPC npc)) {
            return Optional.empty();
        }
        return Optional.ofNullable(NPC_AI.getNPCEntity(npc));
    }

    protected Map<String, String> assign(ConversationWindow conversation, NpcTask task) {
        Optional<NPCEntity> npc = currentNpc(conversation);
        if (npc.isEmpty()) {
            return failure("No NPC is associated with this conversation.");
        }
        boolean assigned = npc.get().getTaskController().assign(npc.get(), task);
        if (!assigned) {
            return failure("The NPC could not start task " + task.name() + ".");
        }
        return Map.of("status", "success", "message", "Task started.", "task", task.name());
    }

    protected Optional<ServerPlayer> findPlayer(String name) {
        if (name == null || name.isBlank() || NPCSystem.server == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(NPCSystem.server.getPlayerList().getPlayer(name));
    }

    protected Optional<NPCEntity> findNpc(String nameOrUuid) {
        if (nameOrUuid == null || nameOrUuid.isBlank()) {
            return Optional.empty();
        }
        return NPC_AI.findNPC(nameOrUuid);
    }

    protected static int secondsToTicks(Object value, int defaultSeconds) {
        if (value == null) {
            return defaultSeconds * 20;
        }
        if (value instanceof Number number) {
            return Math.max(1, number.intValue() * 20);
        }
        try {
            return Math.max(1, Integer.parseInt(value.toString()) * 20);
        } catch (NumberFormatException e) {
            return defaultSeconds * 20;
        }
    }

    protected static Map<String, String> failure(String message) {
        return Map.of("status", "failure", "message", message);
    }
}
