package team.jackdaw.npcsystem.entity.task;

import team.jackdaw.npcsystem.entity.NPCEntity;

public class WaitTask implements NpcTask {
    private final int durationTicks;
    private int ticks;

    public WaitTask(int durationTicks) {
        this.durationTicks = Math.max(1, durationTicks);
    }

    @Override
    public String name() {
        return "wait";
    }

    @Override
    public void start(NPCEntity npc) {
        npc.getNavigation().stop();
    }

    @Override
    public void tick(NPCEntity npc) {
        ticks++;
    }

    @Override
    public boolean isFinished(NPCEntity npc) {
        return ticks >= durationTicks;
    }
}
