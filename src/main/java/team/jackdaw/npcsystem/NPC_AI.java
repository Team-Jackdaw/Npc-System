package team.jackdaw.npcsystem;

import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.ai.brain.ScheduleBuilder;
import net.minecraft.entity.player.PlayerEntity;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.npc.Action;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.Map;
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
        if (npc.getSchedule() == null) {
            return NPCRegistration.SCHEDULE_NPC_DEFAULT;
        }
        Map<Integer, Action> schedule = npc.getSchedule();
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
                entity_window.broadcastMessage();
                entity_window.offWait();
            }
            do {
                if(!entity_window.isOnWait() && !target_window.isOnWait()) {
                    AsyncTask.sleep(3000);
                    entity_window.onWait();
                    target_window.onWait();
                    target_window.chat(entity_window.getLastMessage());
                    target_window.broadcastMessage();
                    AsyncTask.sleep(3000);
                    entity_window.chat(target_window.getLastMessage());
                    entity_window.broadcastMessage();
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
                window.broadcastMessage();
                window.offWait();
            }
            return AsyncTask.nothingToDo();
        });
    }
}
