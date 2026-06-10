package team.jackdaw.npcsystem.entity.task;

import net.minecraft.world.entity.Entity;
import team.jackdaw.npcsystem.entity.NPCEntity;

public class FollowEntityTask extends WalkToEntityTask {
    private final int durationTicks;

    public FollowEntityTask(Entity target, double speed, double stopDistance, int durationTicks) {
        super(target, speed, stopDistance, durationTicks);
        this.durationTicks = Math.max(1, durationTicks);
    }

    @Override
    public String name() {
        return "follow_entity";
    }

    @Override
    public void tick(NPCEntity npc) {
        ticks++;
        if (target != null && target.isAlive()) {
            npc.getLookControl().setLookAt(target, 30.0f, 30.0f);
            if (ticks % 10 == 0 && distance(npc) > stopDistance) {
                npc.getNavigation().moveTo(target, speed);
            } else if (distance(npc) <= stopDistance) {
                npc.getNavigation().stop();
            }
        }
    }

    @Override
    public boolean isFinished(NPCEntity npc) {
        return target == null || !target.isAlive() || !target.level().equals(npc.level()) || ticks >= durationTicks;
    }
}
