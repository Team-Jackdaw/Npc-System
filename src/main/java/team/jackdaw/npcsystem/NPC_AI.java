package team.jackdaw.npcsystem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.master.Master;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;
import team.jackdaw.npcsystem.entity.task.NpcTaskBatchResult;
import team.jackdaw.npcsystem.entity.task.TaskSource;
import team.jackdaw.npcsystem.entity.task.WaitTask;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface NPC_AI {
    BaseManager<UUID, NPCEntity> NPC_ENTITY_MANAGER = new BaseManager<>();

    static NPC getAI(NPCEntity entity) {
        return (NPC) AgentManager.getInstance().get(entity.getUUID());
    }

    static NPCEntity getNPCEntity(NPC npc) {
        return NPC_ENTITY_MANAGER.get(npc.getUUID());
    }

    static Optional<NPCEntity> findNPC(String nameOrUuid) {
        return NPC_ENTITY_MANAGER.map.values().stream()
                .filter(npc -> npc.getUUID().toString().equalsIgnoreCase(nameOrUuid)
                        || npc.getName().getString().equalsIgnoreCase(nameOrUuid))
                .findFirst();
    }

    static void registerNPC(NPCEntity entity) {
        if (!NPC_ENTITY_MANAGER.isRegistered(entity.getUUID())) {
            NPC_ENTITY_MANAGER.register(entity.getUUID(), entity);
        } else if (NPC_ENTITY_MANAGER.get(entity.getUUID()) != entity) {
            NPC_ENTITY_MANAGER.remove(entity.getUUID());
            NPC_ENTITY_MANAGER.register(entity.getUUID(), entity);
        }
        if (!AgentManager.getInstance().isRegistered(entity.getUUID())) {
            AgentManager.getInstance().register(entity.getUUID(), new NPC(entity.getUUID()));
        }
    }

    static void removeNPC(UUID uuid) {
        AgentManager.getInstance().remove(uuid);
        NPC_ENTITY_MANAGER.remove(uuid);
    }

    static void startNPCConversation(NPCEntity entity, NPCEntity target) {
        ConversationWindow entity_window = getAI(entity).getNewConversationWindows();
        ConversationWindow target_window = getAI(target).getNewConversationWindows();
        if (entity_window.isOnWait() || target_window.isOnWait()) {
            return;
        }
        AsyncTask.call(() -> {
            entity_window.setTarget(target.getUUID());
            target_window.setTarget(entity.getUUID());
            if (!entity_window.isOnWait()) {
                entity_window.onWait();
                entity_window.chat();
                broadcastMessage(entity_window);
                entity_window.offWait();
            }
            do {
                if(!entity_window.isOnWait() && !target_window.isOnWait()) {
                    AsyncTask.sleep(3000);
                    entity_window.onWait();
                    target_window.onWait();
                    target_window.chat(entity_window.getLastMessage());
                    broadcastMessage(target_window);
                    AsyncTask.sleep(3000);
                    entity_window.chat(target_window.getLastMessage());
                    broadcastMessage(entity_window);
                    entity_window.offWait();
                    target_window.offWait();
                }
            } while (entity.getBrain().getMemory(NPCRegistration.MEMORY_IS_CHATTING).orElse(false));
            return AsyncTask.nothingToDo();
        });
    }

    static void startPlayerConversation(NPCEntity entity, Player player) {
        if (entity.level().isClientSide()) {
            return;
        }
        NPC npc = getAI(entity);
        if (npc == null) {
            NPCSystem.LOGGER.warn("[npc-system] NPC {} was missing AI registration; registering before conversation.", entity.getUUID());
            registerNPC(entity);
            npc = getAI(entity);
        }
        if (npc == null) {
            NPCSystem.LOGGER.warn("[npc-system] Cannot start conversation for NPC {} because AI registration is unavailable.", entity.getUUID());
            return;
        }
        ConversationWindow window = npc.getNewConversationWindows();
        if (window.isOnWait()) {
            return;
        }
        AsyncTask.call(() -> {
            window.setTarget(player.getUUID());
            if (!window.isOnWait()) {
                window.onWait();
                NPCSystem.debugLog("[npc-system] NPC {} starts player conversation with {}", entity.getUUID(), player.getName().getString());
                window.chat();
                broadcastMessage(window);
                window.offWait();
            }
            // stop if player is not chatting
            AsyncTask.sleep(15000);
            if (!window.isOnWait() || !window.getLastAssistantMessage().isBlank()) {
                entity.getBrain().setMemory(NPCRegistration.MEMORY_IS_CHATTING, false);
            }
            return AsyncTask.nothingToDo();
        });
    }

    static void broadcastMessage(ConversationWindow window) {
        String message = window.getLastMessage();
        if (message.isEmpty()) return;
        Agent agent = window.getAgent();
        if (agent instanceof NPC npc) {
            NPCSystem.debugLog("[npc-system] NPC {} says: {}", npc.getUUID(), message);
            Objects.requireNonNull(NPC_AI.getNPCEntity(npc)).sendMessage(message, Config.range);
        } else if (agent instanceof Master) {
            NPCSystem.debugLog("[npc-system] Master says: {}", message);
            Component message1 = Component.literal("")
                    .append(Component.literal("<Master> ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("").withStyle(ChatFormatting.RESET))
                    .append(Component.literal(window.getLastMessage()));
            Player player = NPCSystem.server.getPlayerList().getPlayer(window.getTarget());
            if (player != null) player.sendSystemMessage(message1);
            else NPCSystem.server.sendSystemMessage(message1);
        }
    }

    static void handleAgentTaskBatchCompleted(NPCEntity entity, NpcTaskBatchResult result) {
        NPC npc = getAI(entity);
        if (npc == null) {
            return;
        }
        if (!entity.getTaskController().isBusy()) {
            entity.getTaskController().assign(entity, new WaitTask(40), TaskSource.SYSTEM);
        }
        ConversationWindow window = npc.getConversationWindows();
        NPCSystem.debugLog("[npc-system] NPC {} completed agent batch {}: {}", entity.getUUID(), result.batchId(), result.summary());
        AsyncTask.call(() -> window.requestAgentFollowUp(result));
    }
}
