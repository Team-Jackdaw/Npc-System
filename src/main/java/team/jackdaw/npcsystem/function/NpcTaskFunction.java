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

    protected Map<String, Object> assign(ConversationWindow conversation, NpcTask task) {
        Optional<NPCEntity> npc = currentNpc(conversation);
        if (npc.isEmpty()) {
            return failure("npc_not_found", "No NPC is associated with this conversation.", false);
        }
        boolean assigned = npc.get().getTaskController().assign(npc.get(), conversation.createAgentTaskAssignment(task));
        if (!assigned) {
            return failure("task_rejected", "The NPC could not start task " + task.name() + ".", true);
        }
        return success("task_started", "Task started.", Map.of("task", task.name()));
    }

    protected Optional<ServerPlayer> findPlayer(String name) {
        if (name == null || name.isBlank() || NPCSystem.server == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(NPCSystem.server.getPlayerList().getPlayer(name));
    }

    protected static String stringArg(Map<String, Object> args, String primary, String... aliases) {
        if (args == null) {
            return null;
        }
        Object value = args.get(primary);
        if (value == null) {
            for (String alias : aliases) {
                value = args.get(alias);
                if (value != null) {
                    break;
                }
            }
        }
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
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

    protected static Map<String, Object> success(String code, String message, Map<String, ?> data) {
        return ToolResult.success(code, message, data);
    }

    protected static Map<String, Object> failure(String code, String message, boolean retryable) {
        return ToolResult.failure(code, message, retryable);
    }
}
