package team.jackdaw.npcsystem.entity.task;

import net.minecraft.world.entity.Entity;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.sensor.NpcSensorState;

import java.util.Comparator;
import java.util.Optional;

public class IdleLookAroundTask implements NpcTask {
    private final int durationTicks;
    private Entity target;
    private int ticks;

    public IdleLookAroundTask(int durationTicks) {
        this.durationTicks = Math.max(1, durationTicks);
    }

    @Override
    public String name() {
        return "idle_look_around";
    }

    @Override
    public boolean canStart(NPCEntity npc) {
        return NpcTask.super.canStart(npc);
    }

    @Override
    public void start(NPCEntity npc) {
        target = findTarget(npc).orElse(null);
    }

    @Override
    public void tick(NPCEntity npc) {
        if (target != null && target.isAlive() && target.level().equals(npc.level())) {
            npc.getLookControl().setLookAt(target, 30.0f, 30.0f);
        }
        ticks++;
    }

    @Override
    public boolean isFinished(NPCEntity npc) {
        return ticks >= durationTicks || target == null || !target.isAlive() || !target.level().equals(npc.level());
    }

    private static Optional<Entity> findTarget(NPCEntity npc) {
        NpcSensorState.Snapshot snapshot = npc.getSensorState().snapshot();
        return snapshot.nearbyPlayers().stream()
                .min(Comparator.comparingDouble(NpcSensorState.EntitySummary::distance))
                .flatMap(summary -> findEntity(npc, summary.uuid()))
                .or(() -> snapshot.nearbyNpcs().stream()
                        .min(Comparator.comparingDouble(NpcSensorState.EntitySummary::distance))
                        .flatMap(summary -> findEntity(npc, summary.uuid())))
                .or(() -> snapshot.nearbyEntities().stream()
                        .min(Comparator.comparingDouble(NpcSensorState.EntitySummary::distance))
                        .flatMap(summary -> findEntity(npc, summary.uuid())));
    }

    private static Optional<Entity> findEntity(NPCEntity npc, String uuid) {
        return npc.level().getEntities(npc, npc.getBoundingBox().inflate(16.0), entity -> entity.getUUID().toString().equals(uuid))
                .stream()
                .findFirst();
    }
}
