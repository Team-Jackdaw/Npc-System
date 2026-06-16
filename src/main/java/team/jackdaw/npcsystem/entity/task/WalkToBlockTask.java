package team.jackdaw.npcsystem.entity.task;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import team.jackdaw.npcsystem.entity.NPCEntity;

public class WalkToBlockTask implements NpcTask {
    private final BlockPos target;
    private final double speed;
    private final double stopDistance;
    private final int timeoutTicks;
    private int ticks;

    public WalkToBlockTask(BlockPos target, double speed, double stopDistance, int timeoutTicks) {
        this.target = target;
        this.speed = speed;
        this.stopDistance = Math.max(0.5, stopDistance);
        this.timeoutTicks = Math.max(1, timeoutTicks);
    }

    @Override
    public String name() {
        return "walk_to_block";
    }

    @Override
    public boolean canStart(NPCEntity npc) {
        return NpcTask.super.canStart(npc) && target != null;
    }

    @Override
    public void start(NPCEntity npc) {
        npc.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
    }

    @Override
    public void tick(NPCEntity npc) {
        ticks++;
        Vec3 center = Vec3.atCenterOf(target);
        npc.getLookControl().setLookAt(center.x, center.y, center.z, 30.0f, 30.0f);
        if (ticks % 20 == 0 && distance(npc) > stopDistance) {
            npc.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
        }
    }

    @Override
    public boolean isFinished(NPCEntity npc) {
        return target == null || distance(npc) <= stopDistance || ticks >= timeoutTicks;
    }

    @Override
    public void stop(NPCEntity npc) {
        npc.getNavigation().stop();
    }

    private double distance(NPCEntity npc) {
        if (target == null) {
            return Double.MAX_VALUE;
        }
        return Math.sqrt(npc.blockPosition().distSqr(target));
    }
}
