package team.jackdaw.npcsystem.entity.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.List;
import java.util.Optional;

// 假定 NPCEntity 是游戏中的NPC实体类，继承自 LivingEntity 或类似类
public class MeetNPCTask extends MultiTickTask<VillagerEntity> {

    // 定义交互持续时间的上下限（以游戏刻为单位，20刻≈1秒）
    private static final int MIN_INTERACTION_DURATION = 600;  // 最少持续200刻（约30秒）， 尝试开始沟通
    private static final int MAX_INTERACTION_DURATION = 2400;  // 最长持续2400刻（约120秒）
    private static final long CD = 2000;  // 交互冷却时间
    private static final double probability = 0.0001;  // 交互概率

    public MeetNPCTask() {
        // 调用父类构造，指定该任务的记忆模块需求：需要LOOK_TARGET和WALK_TARGET，但不需要INTERACTION_TARGET
        super(
                // 使用不可变Map指定需要的记忆状态
                ImmutableMap.of(
                        NPCRegistration.MEMORY_NEAREST_NPC, MemoryModuleState.VALUE_PRESENT,
                        NPCRegistration.MEMORY_CHATTING_TARGET, MemoryModuleState.VALUE_ABSENT,
                        NPCRegistration.MEMORY_IS_CHATTING, MemoryModuleState.REGISTERED,
                        NPCRegistration.MEMORY_LAST_CHAT_TIME, MemoryModuleState.REGISTERED,
                        MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_ABSENT,
                        MemoryModuleType.WALK_TARGET, MemoryModuleState.REGISTERED,
                        MemoryModuleType.LOOK_TARGET, MemoryModuleState.REGISTERED
                ),
                MIN_INTERACTION_DURATION,
                MAX_INTERACTION_DURATION
        );
        // 父类 MultiTickTask 可能会根据提供的持续时间范围自动处理持续时间，这里传入最小和最大刻数
    }

    /**
     * 判断任务是否该开始执行。只有当NPC的记忆中存在最近可见且可交互的NPC时，任务才会触发。
     */
    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
        if (Math.random() > probability) return false;
        Brain<?> npcBrain = entity.getBrain();
        Optional<List<NPCEntity>> targetOpt = npcBrain.getOptionalRegisteredMemory(NPCRegistration.MEMORY_NEAREST_NPC);
        // 同时确保当前没有正在交互的目标（避免重复触发）
        return npcBrain.isMemoryInState(MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_ABSENT)
                && npcBrain.isMemoryInState(NPCRegistration.MEMORY_CHATTING_TARGET, MemoryModuleState.VALUE_ABSENT)
                && !npcBrain.getOptionalRegisteredMemory(NPCRegistration.MEMORY_IS_CHATTING).orElse(false)
                && world.getTime() - npcBrain.getOptionalRegisteredMemory(NPCRegistration.MEMORY_LAST_CHAT_TIME).orElse(0L) >= CD
                && targetOpt.orElse(List.of()).stream().anyMatch(target -> {
            Brain<?> targetBrain = target.getBrain();
            return targetBrain.isMemoryInState(MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_ABSENT)
                    && targetBrain.isMemoryInState(NPCRegistration.MEMORY_CHATTING_TARGET, MemoryModuleState.VALUE_ABSENT)
                    && !targetBrain.getOptionalRegisteredMemory(NPCRegistration.MEMORY_IS_CHATTING).orElse(false)
                    && world.getTime() - targetBrain.getOptionalRegisteredMemory(NPCRegistration.MEMORY_LAST_CHAT_TIME).orElse(0L) >= CD;
        });
    }

    /**
     * 当任务开始时执行。设置交互目标并启动对话。
     */
    @Override
    protected void run(ServerWorld world, VillagerEntity entity, long gameTime) {
        NPCEntity npc = (NPCEntity) entity;
        Optional<NPCEntity> targetOpt = npc.getBrain()
                .getOptionalRegisteredMemory(NPCRegistration.MEMORY_NEAREST_NPC)
                .orElse(List.of())
                .stream()
                .filter(target -> {
                    Brain<?> targetBrain = target.getBrain();
                    return targetBrain.isMemoryInState(MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_ABSENT)
                            && targetBrain.isMemoryInState(NPCRegistration.MEMORY_CHATTING_TARGET, MemoryModuleState.VALUE_ABSENT)
                            && !targetBrain.getOptionalRegisteredMemory(NPCRegistration.MEMORY_IS_CHATTING).orElse(false)
                            && world.getTime() - targetBrain.getOptionalRegisteredMemory(NPCRegistration.MEMORY_LAST_CHAT_TIME).orElse(0L) >= CD;
                }).findFirst();
        if (targetOpt.isEmpty()) {
            return;
        }
        NPCEntity targetNPC = targetOpt.get();
        // 设定前进目标和视线以及交互目标
        LookTargetUtil.lookAtAndWalkTowardsEachOther(npc, targetNPC, 0.5f);
        npc.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, targetNPC);
        npc.getBrain().remember(NPCRegistration.MEMORY_CHATTING_TARGET, targetNPC);
        npc.getBrain().remember(NPCRegistration.MEMORY_IS_CHATTING, true);
        targetNPC.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, npc);
        targetNPC.getBrain().remember(NPCRegistration.MEMORY_CHATTING_TARGET, npc);
        targetNPC.getBrain().remember(NPCRegistration.MEMORY_IS_CHATTING, true);

        // 通过接口调用触发NPC之间的对话交互
        NPC_AI.startNPCConversation(npc, targetNPC);
    }

    /**
     * 每个游戏刻都会调用，用于判断任务是否应继续保持活动状态。
     */
    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity npc, long gameTime) {
        // 检查：
        // 1. NPC自身仍然有交互目标（对话对象）记录，表示对话未中断
        // 2. 当前游戏时间尚未超过计划的结束时间
        Optional<Entity> targetOpt = npc.getBrain().getOptionalRegisteredMemory(NPCRegistration.MEMORY_CHATTING_TARGET);
        if (targetOpt.isEmpty()) {
            return false;
        }
        NPCEntity targetNPC = (NPCEntity) targetOpt.get();
        return npc.getBrain().isMemoryInState(NPCRegistration.MEMORY_CHATTING_TARGET, MemoryModuleState.VALUE_PRESENT)
                && npc.getBrain().getOptionalRegisteredMemory(NPCRegistration.MEMORY_IS_CHATTING).orElse(false)
                && targetNPC.getBrain().isMemoryInState(NPCRegistration.MEMORY_CHATTING_TARGET, MemoryModuleState.VALUE_PRESENT)
                && targetNPC.getBrain().getOptionalRegisteredMemory(NPCRegistration.MEMORY_IS_CHATTING).orElse(false);
    }

    @Override
    protected void keepRunning(ServerWorld world, VillagerEntity npc, long gameTime) {
        NPCEntity targetNPC = (NPCEntity) npc.getBrain().getOptionalRegisteredMemory(NPCRegistration.MEMORY_CHATTING_TARGET).get();
        LookTargetUtil.lookAtAndWalkTowardsEachOther(npc, targetNPC, 0.5f);
        npc.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, targetNPC);
        targetNPC.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, npc);
    }

    /**
     * 当任务结束（不再继续执行）时调用，用于清理状态。
     * 这包括正常结束和因中断而提前结束的情况。
     */
    @Override
    protected void finishRunning(ServerWorld world, VillagerEntity npc, long gameTime) {
        NPCEntity targetNPC = (NPCEntity) npc.getBrain().getOptionalRegisteredMemory(NPCRegistration.MEMORY_CHATTING_TARGET).get();
        // 清除交互目标记忆
        npc.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
        npc.getBrain().forget(NPCRegistration.MEMORY_CHATTING_TARGET);
        npc.getBrain().forget(NPCRegistration.MEMORY_IS_CHATTING);
        npc.getBrain().remember(NPCRegistration.MEMORY_LAST_CHAT_TIME, world.getTime());
        targetNPC.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
        targetNPC.getBrain().forget(NPCRegistration.MEMORY_CHATTING_TARGET);
        targetNPC.getBrain().forget(NPCRegistration.MEMORY_IS_CHATTING);
        targetNPC.getBrain().remember(NPCRegistration.MEMORY_LAST_CHAT_TIME, world.getTime());
        // （此处不需要显式修改NPC的大脑活动，因为清除交互目标后，
        //  大脑会根据剩余的MEET活动任务自动选择下一个行为）
    }
}
