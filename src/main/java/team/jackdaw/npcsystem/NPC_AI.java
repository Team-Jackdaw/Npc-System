package team.jackdaw.npcsystem;

import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.ai.brain.ScheduleBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.jetbrains.annotations.Nullable;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.npc.Action;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NPC_AI {
    BaseManager<UUID, NPCEntity> NPC_ENTITY_MANAGER = new BaseManager<>();

    static @Nullable NPC getAI(NPCEntity entity) {
        return (NPC) AgentManager.getInstance().get(entity.getUuid());
    }

    static @Nullable NPCEntity getNPCEntity(NPC npc) {
        return NPC_ENTITY_MANAGER.get(npc.getUUID());
    }

    static NPC getOrRegisterNPC(NPCEntity entity) {
        NPCEntity entity1 = NPC_ENTITY_MANAGER.get(entity.getUuid());
        if (entity1 == null) {
            NPC_ENTITY_MANAGER.register(entity.getUuid(), entity);
        } else if (!entity1.equals(entity)) {
            NPC_ENTITY_MANAGER.remove(entity.getUuid());
            NPC_ENTITY_MANAGER.register(entity.getUuid(), entity);
        }
        NPC ai = (NPC) AgentManager.getInstance().get(entity.getUuid());
        if (ai == null) {
            ai = new NPC(entity.getUuid());
            AgentManager.getInstance().register(ai);
        }
        return ai;
    }

    static void removeNPC(NPCEntity entity) {
        AgentManager.getInstance().remove(entity.getUuid());
        NPC_ENTITY_MANAGER.remove(entity.getUuid());
    }

    static Schedule getSchedule(NPCEntity entity) {
        return getSchedule(getOrRegisterNPC(entity));
    }

    static Schedule getSchedule(NPC npc) {
        if (npc.getSchedule() == null) {
            return NPCRegistration.SCHEDULE_NPC_DEFAULT;
        }
        Map<Integer, Action> schedule = npc.getSchedule();
        ScheduleBuilder builder = new ScheduleBuilder(Registry.register(Registries.SCHEDULE, "npc_" + npc.getUUID(), new Schedule()));
        schedule.forEach((time, action) -> builder.withActivity(time, activityMapping(action)));
        return builder.build();
    }

    static Activity activityMapping(Action action) {
        // TODO: create a mapping from Action to Activity
        return null;
    }

    static void startNPCConversation(NPCEntity entity, NPCEntity target, Runnable callback) {
        AsyncTask.call(() -> {
            try {
                // TODO: implement this method
                entity.sendMessage("Hello, " + target.getName().getString() + "!", 10);
                entity.wait(5000);
                target.sendMessage("Hello, " + entity.getName().getString() + "!", 10);
            } catch (Exception e) {
                NPCSystem.LOGGER.error("Conversation was interrupted!", e);
            }
            callback.run();
            return AsyncTask.nothingToDo();
        });

    }

    static NPCEntity getNearestNPC(NPCEntity entity) {
        List<NPCEntity> list = NPC_ENTITY_MANAGER.map.values().stream().filter(npc -> npc != entity).toList();
        if (!list.isEmpty()) {
            return list.get(0);
        } else {
            return null;
        }
    }
}
