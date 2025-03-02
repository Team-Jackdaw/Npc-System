package team.jackdaw.npcsystem;

import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.ai.brain.ScheduleBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.master.Master;
import team.jackdaw.npcsystem.ai.npc.Action;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public interface NPC_AI {
    BaseManager<UUID, NPCEntity> NPC_ENTITY_MANAGER = new BaseManager<>();

    static NPC getAI(NPCEntity entity) {
        return (NPC) AgentManager.getInstance().get(entity.getUuid());
    }

    static NPCEntity getNPCEntity(NPC npc) {
        return NPC_ENTITY_MANAGER.get(npc.getUUID());
    }

    static void registerNPC(NPCEntity entity) {
        if (!NPC_ENTITY_MANAGER.isRegistered(entity.getUuid())) {
            NPC_ENTITY_MANAGER.register(entity.getUuid(), entity);
        } else if (NPC_ENTITY_MANAGER.get(entity.getUuid()) != entity) {
            NPC_ENTITY_MANAGER.remove(entity.getUuid());
            NPC_ENTITY_MANAGER.register(entity.getUuid(), entity);
        }
        if (!AgentManager.getInstance().isRegistered(entity.getUuid())) {
            AgentManager.getInstance().register(entity.getUuid(), new NPC(entity.getUuid()));
        }
    }

    static void removeNPC(UUID uuid) {
        AgentManager.getInstance().remove(uuid);
        NPC_ENTITY_MANAGER.remove(uuid);
    }

    static Schedule getSchedule(NPCEntity entity) {
        NPC ai = getAI(entity);
        if (ai == null) {
            return NPCRegistration.SCHEDULE_NPC_DEFAULT;
        } else {
            return getSchedule(ai);
        }
    }

    static Schedule getSchedule(NPC npc) {
        Map<Integer, Action> schedule = npc.getSchedule();
        if (schedule == null) {
            return NPCRegistration.SCHEDULE_NPC_DEFAULT;
        }
        ScheduleBuilder builder = new ScheduleBuilder(new Schedule());
        schedule.forEach((time, action) -> builder.withActivity(time, activityMapping(action)));
        return builder.build();
    }

    static Activity activityMapping(Action action) {
        // TODO: create a mapping from Action to Activity
        return null;
    }

    static void startNPCConversation(NPCEntity entity, NPCEntity target) {
        ConversationWindow entity_window = getAI(entity).getNewConversationWindows();
        ConversationWindow target_window = getAI(target).getNewConversationWindows();
        if (entity_window.isOnWait() || target_window.isOnWait()) {
            return;
        }
        AsyncTask.call(() -> {
            entity_window.setTarget(target.getUuid());
            target_window.setTarget(entity.getUuid());
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
            } while (entity.getBrain().getOptionalRegisteredMemory(NPCRegistration.MEMORY_IS_CHATTING).orElse(false));
            return AsyncTask.nothingToDo();
        });
    }

    static void startPlayerConversation(NPCEntity entity, PlayerEntity player) {
        ConversationWindow window = getAI(entity).getNewConversationWindows();
        if (window.isOnWait()) {
            return;
        }
        AsyncTask.call(() -> {
            window.setTarget(player.getUuid());
            if (!window.isOnWait()) {
                window.onWait();
                window.chat();
                broadcastMessage(window);
                window.offWait();
            }
            // stop if player is not chatting
            AsyncTask.sleep(15000);
            if (!window.isOnWait() || window.getMessages().get(window.getMessages().size() - 1).role.equals("assistant")) {
                entity.getBrain().remember(NPCRegistration.MEMORY_IS_CHATTING, false);
            }
            return AsyncTask.nothingToDo();
        });
    }

    static void broadcastMessage(ConversationWindow window) {
        String message = window.getLastMessage();
        if (message.isEmpty()) return;
        Agent agent = window.getAgent();
        if (agent instanceof NPC npc) {
            Objects.requireNonNull(NPC_AI.getNPCEntity(npc)).sendMessage(message, Config.range);
        } else if (agent instanceof Master) {
            Text message1 = Text.literal("")
                    .append(Text.literal("<Master> ").formatted(Formatting.RED))
                    .append("").formatted(Formatting.RESET)
                    .append(Text.of(window.getLastMessage()));
            PlayerEntity player = NPCSystem.server.getPlayerManager().getPlayer(window.getTarget());
            if (player != null) player.sendMessage(message1);
            else NPCSystem.server.sendMessage(message1);
        }
    }
}
