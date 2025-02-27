package team.jackdaw.npcsystem.entity.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.List;
import java.util.Optional;

public class MeetPlayerTask extends MultiTickTask<VillagerEntity> {
    private static final int DURATION = 5000;
    private static final long CD = 2000;
    private static final double probability = 0.0001;

    public MeetPlayerTask() {
        super(
                ImmutableMap.of(
                        NPCRegistration.MEMORY_CHATTING_TARGET, MemoryModuleState.VALUE_ABSENT,
                        NPCRegistration.MEMORY_IS_CHATTING, MemoryModuleState.REGISTERED,
                        NPCRegistration.MEMORY_LAST_CHAT_TIME, MemoryModuleState.REGISTERED,
                        MemoryModuleType.NEAREST_PLAYERS, MemoryModuleState.VALUE_PRESENT,
                        MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_ABSENT,
                        MemoryModuleType.WALK_TARGET, MemoryModuleState.REGISTERED,
                        MemoryModuleType.LOOK_TARGET, MemoryModuleState.REGISTERED
                ),
                DURATION
        );
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
        if (Math.random() > probability) return false;
        Brain<?> npcBrain = entity.getBrain();
        Optional<List<PlayerEntity>> targetOpt = npcBrain.getOptionalRegisteredMemory(MemoryModuleType.NEAREST_PLAYERS);
        return npcBrain.isMemoryInState(MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_ABSENT)
                && npcBrain.isMemoryInState(NPCRegistration.MEMORY_CHATTING_TARGET, MemoryModuleState.VALUE_ABSENT)
                && !npcBrain.getOptionalRegisteredMemory(NPCRegistration.MEMORY_IS_CHATTING).orElse(false)
                && world.getTime() - npcBrain.getOptionalRegisteredMemory(NPCRegistration.MEMORY_LAST_CHAT_TIME).orElse(0L) >= CD
                && !targetOpt.orElse(List.of()).isEmpty();
    }

    @Override
    protected void run(ServerWorld world, VillagerEntity entity, long gameTime) {
        Optional<List<PlayerEntity>> targetOpt = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.NEAREST_PLAYERS);
        PlayerEntity player = targetOpt.orElse(List.of()).get(0);
        LookTargetUtil.lookAt(entity, player);
        LookTargetUtil.walkTowards(entity, player, 0.5f, 2);
        entity.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, player);
        entity.getBrain().remember(NPCRegistration.MEMORY_CHATTING_TARGET, player);
        entity.getBrain().remember(NPCRegistration.MEMORY_IS_CHATTING, true);
        NPC_AI.startPlayerConversation((NPCEntity) entity, player);
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity npc, long gameTime) {
        return npc.getBrain().isMemoryInState(NPCRegistration.MEMORY_CHATTING_TARGET, MemoryModuleState.VALUE_PRESENT)
                && npc.getBrain().getOptionalRegisteredMemory(NPCRegistration.MEMORY_IS_CHATTING).orElse(false);
    }

    @Override
    protected void keepRunning(ServerWorld world, VillagerEntity entity, long gameTime) {
        PlayerEntity player = (PlayerEntity) entity.getBrain().getOptionalRegisteredMemory(NPCRegistration.MEMORY_CHATTING_TARGET).get();
        LookTargetUtil.lookAt(entity, player);
        LookTargetUtil.walkTowards(entity, player, 0.5f, 2);
        entity.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, player);
    }

    @Override
    protected void finishRunning(ServerWorld world, VillagerEntity npc, long gameTime) {
        npc.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
        npc.getBrain().forget(NPCRegistration.MEMORY_CHATTING_TARGET);
        npc.getBrain().forget(NPCRegistration.MEMORY_IS_CHATTING);
        npc.getBrain().remember(NPCRegistration.MEMORY_LAST_CHAT_TIME, world.getTime());
    }
}
