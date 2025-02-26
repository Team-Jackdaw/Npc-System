package team.jackdaw.npcsystem.entity.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.Optional;

// 假定 NPCEntity 是游戏中的NPC实体类，继承自 LivingEntity 或类似类
public class MeetNPCTask extends MultiTickTask<VillagerEntity> {

    // 定义交互持续时间的上下限（以游戏刻为单位，20刻≈1秒）
    private static final int MIN_INTERACTION_DURATION = 600;  // 最少持续200刻（约30秒）， 尝试开始沟通
    private static final int MAX_INTERACTION_DURATION = 2400;  // 最长持续2400刻（约120秒）

    // 用于记录正在交互的目标NPC和交互结束的时间
    private NPCEntity targetNPC;
    private long endTime;
    private boolean completed = false;

    public MeetNPCTask() {
        // 调用父类构造，指定该任务的记忆模块需求：
        // 需要存在 MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC，并且当前没有正在交互的对象 (INTERACTION_TARGET为空)。
        super(
                // 使用不可变Map指定需要的记忆状态
                ImmutableMap.of(
                        NPCRegistration.MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC, MemoryModuleState.VALUE_PRESENT,
                        MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_ABSENT,
                        MemoryModuleType.LOOK_TARGET, MemoryModuleState.REGISTERED,
                        MemoryModuleType.WALK_TARGET, MemoryModuleState.REGISTERED
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
    protected boolean shouldRun(ServerWorld world, VillagerEntity npc) {
        Brain<?> brain = npc.getBrain();
        // 当存在“最近可见且可作为交互目标的NPC”记忆时，触发任务开始
        // 同时确保当前没有正在交互的目标（避免重复触发）
        return brain.hasMemoryModule(NPCRegistration.MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC)
                && brain.hasMemoryModule(MemoryModuleType.INTERACTION_TARGET);
    }

    /**
     * 当任务开始时执行。设置交互目标并启动对话。
     */
    @Override
    protected void run(ServerWorld world, VillagerEntity npc, long gameTime) {
        Brain<?> brain = npc.getBrain();
        // 从记忆中获取最近可见的目标NPC实体
        Optional<LivingEntity> targetOpt = brain.getOptionalRegisteredMemory(NPCRegistration.MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC);
        if (targetOpt.isEmpty()) {
            return;  // 没有目标则不执行
        }
        targetNPC = (NPCEntity) targetOpt.get();

        // 设定前进目标和视线
        brain.remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(targetNPC, true));
        brain.remember(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityLookTarget(targetNPC, false), 0.3f, 1));
        targetNPC.getBrain().remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(npc, true));
        targetNPC.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityLookTarget(npc, false), 0.3f, 1));

        // 将双方设置为彼此的交互目标
        brain.remember(MemoryModuleType.INTERACTION_TARGET, targetNPC);
        targetNPC.getBrain().remember(MemoryModuleType.INTERACTION_TARGET, npc);

        // 通过接口调用触发NPC之间的对话交互
        NPC_AI.startNPCConversation((NPCEntity) npc, targetNPC, this::callBack);

        // 随机决定此次交互的持续时间，在最小和最大范围之间（这里为5到10秒之间）
        int durationTicks = MIN_INTERACTION_DURATION + npc.getRandom().nextInt(
                MAX_INTERACTION_DURATION - MIN_INTERACTION_DURATION + 1);
        // 记录交互结束的时间点（当前游戏时间 + 持续时间）
        endTime = gameTime + durationTicks;
    }

    private void callBack() {
        completed = true;
    }

    /**
     * 每个游戏刻都会调用，用于判断任务是否应继续保持活动状态。
     */
    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity npc, long gameTime) {
        Brain<?> brain = npc.getBrain();
        // 检查：
        // 1. NPC自身仍然有交互目标（对话对象）记录，表示对话未中断
        // 2. 当前游戏时间尚未超过计划的结束时间
        boolean stillInteracting = brain.hasMemoryModuleWithValue(MemoryModuleType.INTERACTION_TARGET, targetNPC);
        boolean withinTimeLimit = (gameTime < endTime);
        return stillInteracting && withinTimeLimit && !completed;
    }

    /**
     * 当任务结束（不再继续执行）时调用，用于清理状态。
     * 这包括正常结束和因中断而提前结束的情况。
     */
    @Override
    protected void finishRunning(ServerWorld world, VillagerEntity npc, long gameTime) {
        Brain<?> brain = npc.getBrain();
        // 清除自身的交互目标记忆
        brain.forget(MemoryModuleType.INTERACTION_TARGET);
        brain.forget(MemoryModuleType.LOOK_TARGET);
        brain.forget(MemoryModuleType.WALK_TARGET);
        // 清除目标NPC的交互目标记忆（对方也不再认为正在交互）
        if (targetNPC != null) {
            targetNPC.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
            targetNPC.getBrain().forget(MemoryModuleType.LOOK_TARGET);
            targetNPC.getBrain().forget(MemoryModuleType.WALK_TARGET);
        }
        // 重置目标引用
        targetNPC = null;
        // （此处不需要显式修改NPC的大脑活动，因为清除交互目标后，
        //  大脑会根据剩余的MEET活动任务自动选择下一个行为）
    }
}
