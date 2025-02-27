package team.jackdaw.npcsystem.entity.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import team.jackdaw.npcsystem.entity.NPCRegistration;

public class MeetPlayerTask extends MultiTickTask<VillagerEntity> {
    private static final int DURATION = 1000;
    private static final long CD = 2000;
    private static final double probability = 0.0001;

    public MeetPlayerTask() {
        super(
                ImmutableMap.of(
                        NPCRegistration.MEMORY_CHATTING_TARGET, MemoryModuleState.VALUE_ABSENT,
                        NPCRegistration.MEMORY_IS_CHATTING, MemoryModuleState.REGISTERED,
                        NPCRegistration.MEMORY_LAST_CHAT_TIME, MemoryModuleState.REGISTERED,
                        MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_ABSENT,
                        MemoryModuleType.WALK_TARGET, MemoryModuleState.REGISTERED,
                        MemoryModuleType.LOOK_TARGET, MemoryModuleState.REGISTERED
                ),
                DURATION
        );
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
        return false;
    }

    @Override
    protected void run(ServerWorld world, VillagerEntity entity, long gameTime) {
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity npc, long gameTime) {
        return false;
    }

    @Override
    protected void keepRunning(ServerWorld world, VillagerEntity npc, long gameTime) {
    }

    @Override
    protected void finishRunning(ServerWorld world, VillagerEntity npc, long gameTime) {
    }
}
