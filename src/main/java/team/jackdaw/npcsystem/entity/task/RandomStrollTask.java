package team.jackdaw.npcsystem.entity.task;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import team.jackdaw.npcsystem.entity.NPCEntity;

public class RandomStrollTask implements NpcTask {
    private static final int RADIUS = 8;
    private final double speed;
    private final int timeoutTicks;
    private Vec3 target;
    private int ticks;

    public RandomStrollTask(double speed, int timeoutTicks) {
        this.speed = speed;
        this.timeoutTicks = Math.max(1, timeoutTicks);
    }

    @Override
    public String name() {
        return "random_stroll";
    }

    @Override
    public boolean canStart(NPCEntity npc) {
        return NpcTask.super.canStart(npc);
    }

    @Override
    public void start(NPCEntity npc) {
        target = chooseTarget(npc);
        if (target != null) {
            npc.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }

    @Override
    public void tick(NPCEntity npc) {
        ticks++;
        if (target != null && ticks % 20 == 0) {
            npc.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }

    @Override
    public boolean isFinished(NPCEntity npc) {
        return target == null || ticks >= timeoutTicks || npc.distanceToSqr(target) <= 2.25;
    }

    @Override
    public void stop(NPCEntity npc) {
        npc.getNavigation().stop();
    }

    private static Vec3 chooseTarget(NPCEntity npc) {
        for (int i = 0; i < 8; i++) {
            int x = npc.getRandom().nextInt(RADIUS * 2 + 1) - RADIUS;
            int z = npc.getRandom().nextInt(RADIUS * 2 + 1) - RADIUS;
            BlockPos pos = npc.blockPosition().offset(x, 0, z);
            BlockPos ground = npc.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
            if (npc.level().getBlockState(ground.below()).isSolidRender() && npc.level().getBlockState(ground).isAir()) {
                return Vec3.atBottomCenterOf(ground);
            }
        }
        return null;
    }
}
