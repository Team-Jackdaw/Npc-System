package team.jackdaw.npcsystem.entity.task;

import team.jackdaw.npcsystem.entity.NPCEntity;

public class DefaultBehaviorController {
    private static final int MIN_INTERVAL_TICKS = 60;
    private static final int INTERVAL_RANGE_TICKS = 100;
    private long nextDecisionTick;

    public void tick(NPCEntity npc) {
        NpcTaskController controller = npc.getTaskController();
        if (!controller.canRunDefaultTask()) {
            return;
        }
        long gameTime = npc.level().getGameTime();
        if (gameTime < nextDecisionTick) {
            return;
        }
        controller.assign(npc, chooseTask(npc), TaskSource.DEFAULT);
        nextDecisionTick = gameTime + MIN_INTERVAL_TICKS + npc.getRandom().nextInt(INTERVAL_RANGE_TICKS + 1);
    }

    private static NpcTask chooseTask(NPCEntity npc) {
        int roll = npc.getRandom().nextInt(100);
        if (roll < 40 && hasLookTarget(npc)) {
            return new IdleLookAroundTask(40 + npc.getRandom().nextInt(61));
        }
        if (roll < 80) {
            return new RandomStrollTask(0.45, 120);
        }
        return new WaitTask(40 + npc.getRandom().nextInt(61));
    }

    private static boolean hasLookTarget(NPCEntity npc) {
        var snapshot = npc.getSensorState().snapshot();
        return !snapshot.nearbyPlayers().isEmpty()
                || !snapshot.nearbyNpcs().isEmpty()
                || !snapshot.nearbyEntities().isEmpty();
    }
}
