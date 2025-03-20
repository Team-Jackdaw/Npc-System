package team.jackdaw.npcsystem.entity.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import team.jackdaw.npcsystem.entity.NPCRegistration;

public class FollowChatTargetTask extends MultiTickTask<VillagerEntity> {
    private final float speed;
    public FollowChatTargetTask(float speed) {
        super(ImmutableMap.of(
                NPCRegistration.MEMORY_CHATTING_TARGET, MemoryModuleState.VALUE_PRESENT,
                NPCRegistration.MEMORY_IS_CHATTING, MemoryModuleState.VALUE_PRESENT,
                MemoryModuleType.WALK_TARGET, MemoryModuleState.REGISTERED, 
                MemoryModuleType.LOOK_TARGET, MemoryModuleState.REGISTERED
        ), Integer.MAX_VALUE);
        this.speed = speed;
    }

    @Override
    protected boolean shouldRun(ServerWorld serverWorld, VillagerEntity villagerEntity) {
        Entity entity = villagerEntity.getBrain().getOptionalRegisteredMemory(NPCRegistration.MEMORY_CHATTING_TARGET).orElse(null);
        boolean isChatting = villagerEntity.getBrain().getOptionalRegisteredMemory(NPCRegistration.MEMORY_IS_CHATTING).orElse(false);
        return isChatting && entity != null && villagerEntity.isAlive() && !villagerEntity.isTouchingWater() && !villagerEntity.velocityModified && villagerEntity.squaredDistanceTo(entity) <= 16.0;
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        return this.shouldRun(serverWorld, villagerEntity);
    }

    @Override
    protected void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        this.update(villagerEntity);
    }

    @Override
    protected void finishRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        Brain<VillagerEntity> brain = villagerEntity.getBrain();
        brain.forget(MemoryModuleType.WALK_TARGET);
        brain.forget(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void keepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long l) {
        this.update(villagerEntity);
    }

    private void update(VillagerEntity villager) {
        Entity target = villager.getBrain().getOptionalRegisteredMemory(NPCRegistration.MEMORY_CHATTING_TARGET).orElse(null);
        if (target == null) return;
        LookTargetUtil.lookAt(villager, (LivingEntity) target);
        LookTargetUtil.walkTowards(villager, target, 0.5f, 2);
    }
}
