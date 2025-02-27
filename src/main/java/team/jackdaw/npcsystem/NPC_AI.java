package team.jackdaw.npcsystem;

import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.ai.brain.ScheduleBuilder;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.npc.Action;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.lang.Thread.sleep;

public interface NPC_AI {
    BaseManager<UUID, NPCEntity> NPC_ENTITY_MANAGER = new BaseManager<>();

    static @Nullable NPC getAI(NPCEntity entity) {
        return (NPC) AgentManager.getInstance().get(entity.getUuid());
    }

    static @Nullable NPCEntity getNPCEntity(NPC npc) {
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
        AsyncTask.call(() -> {
            try {
                // TODO: implement this method
                for (int i = 0 ; i < 5 ; i++ ) {
                    Optional<Boolean> isChatting = entity.getBrain().getOptionalRegisteredMemory(NPCRegistration.MEMORY_IS_CHATTING);
                    if (entity.isRemoved() || target.isRemoved() || !isChatting.orElse(false)) {
                        break;
                    }
                    entity.sendMessage("Hello, " + Optional.ofNullable(target.getCustomName()).orElse(Text.of("Someone")).getString() + "!" + i, Config.range);
                    sleep(5000);
                    target.sendMessage("Hello, " + Optional.ofNullable(entity.getCustomName()).orElse(Text.of("Someone")).getString() + "!" + i, Config.range);
                    sleep(5000);
                }
                entity.getBrain().remember(NPCRegistration.MEMORY_IS_CHATTING, false);
            } catch (Exception e) {
                NPCSystem.LOGGER.error("Conversation was interrupted!", e);
                entity.getBrain().remember(NPCRegistration.MEMORY_IS_CHATTING, false);
            }
            return AsyncTask.nothingToDo();
        });

    }
}
