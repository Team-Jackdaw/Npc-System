package team.jackdaw.npcsystem.entity.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.task.SingleTickTask;
import net.minecraft.entity.ai.brain.task.TaskTriggerer;
import net.minecraft.util.math.GlobalPos;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

public class MeetNPCTask{

    public MeetNPCTask() {
    }

    public static SingleTickTask<LivingEntity> create() {
        return TaskTriggerer.task(
                (context) -> context
                        // Memories that required for the task
                        .group(
                                context.queryMemoryOptional(MemoryModuleType.WALK_TARGET),
                                context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
                                context.queryMemoryValue(MemoryModuleType.MEETING_POINT),
                                context.queryMemoryValue(NPCRegistration.MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC),
                                context.queryMemoryAbsent(MemoryModuleType.INTERACTION_TARGET)
                        )
                        .apply(context, (walkTarget, lookTarget, meetingPoint, npc, interactionTarget) -> (world, entity, time) -> {
                            GlobalPos globalPos = context.getValue(meetingPoint);
                            NPCEntity otherNPC = (NPCEntity) context.getValue(npc);
                            if (world.getRandom().nextInt(100) == 0
                                    && world.getRegistryKey() == globalPos.getDimension()
                                    && globalPos.getPos().isWithinDistance(entity.getPos(), 4.0)
                            ) {
                                interactionTarget.remember(otherNPC);
                                lookTarget.remember(new EntityLookTarget(otherNPC, true));
                                walkTarget.remember(new WalkTarget(new EntityLookTarget(otherNPC, false), 0.3F, 1));
                                return true;
                            } else {
                                return false;
                            }
                        }));
    }
}
