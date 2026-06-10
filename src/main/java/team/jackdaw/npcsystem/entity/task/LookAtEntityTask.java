package team.jackdaw.npcsystem.entity.task;

import net.minecraft.world.entity.Entity;
import team.jackdaw.npcsystem.entity.NPCEntity;

public class LookAtEntityTask implements NpcTask {
    private final Entity target;
    private final int durationTicks;
    private int ticks;

    public LookAtEntityTask(Entity target, int durationTicks) {
        this.target = target;
        this.durationTicks = Math.max(1, durationTicks);
    }

    @Override
    public String name() {
        return "look_at_entity";
    }

    @Override
    public boolean canStart(NPCEntity npc) {
        return NpcTask.super.canStart(npc) && target != null && target.isAlive() && target.level().equals(npc.level());
    }

    @Override
    public void tick(NPCEntity npc) {
        if (target != null && target.isAlive()) {
            npc.getLookControl().setLookAt(target, 30.0f, 30.0f);
        }
        ticks++;
    }

    @Override
    public boolean isFinished(NPCEntity npc) {
        return ticks >= durationTicks || target == null || !target.isAlive() || !target.level().equals(npc.level());
    }
}
