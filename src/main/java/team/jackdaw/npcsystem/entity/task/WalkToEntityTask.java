package team.jackdaw.npcsystem.entity.task;

import net.minecraft.world.entity.Entity;
import team.jackdaw.npcsystem.entity.NPCEntity;

public class WalkToEntityTask implements NpcTask {
    protected final Entity target;
    protected final double speed;
    protected final double stopDistance;
    protected final int timeoutTicks;
    protected int ticks;

    public WalkToEntityTask(Entity target, double speed, double stopDistance, int timeoutTicks) {
        this.target = target;
        this.speed = speed;
        this.stopDistance = stopDistance;
        this.timeoutTicks = Math.max(1, timeoutTicks);
    }

    @Override
    public String name() {
        return "walk_to_entity";
    }

    @Override
    public boolean canStart(NPCEntity npc) {
        return NpcTask.super.canStart(npc) && target != null && target.isAlive() && target.level().equals(npc.level());
    }

    @Override
    public void start(NPCEntity npc) {
        npc.getNavigation().moveTo(target, speed);
    }

    @Override
    public void tick(NPCEntity npc) {
        ticks++;
        if (target != null && target.isAlive()) {
            npc.getLookControl().setLookAt(target, 30.0f, 30.0f);
            if (ticks % 20 == 0 && distance(npc) > stopDistance) {
                npc.getNavigation().moveTo(target, speed);
            }
        }
    }

    @Override
    public boolean isFinished(NPCEntity npc) {
        return target == null || !target.isAlive() || !target.level().equals(npc.level()) || distance(npc) <= stopDistance || ticks >= timeoutTicks;
    }

    @Override
    public void stop(NPCEntity npc) {
        npc.getNavigation().stop();
    }

    protected double distance(NPCEntity npc) {
        if (target == null) {
            return Double.MAX_VALUE;
        }
        return Math.sqrt(npc.distanceToSqr(target));
    }
}
