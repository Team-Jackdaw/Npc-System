package team.jackdaw.npcsystem.entity.sensor;

import team.jackdaw.npcsystem.entity.NPCEntity;

public final class ObservationCollector {
    private ObservationCollector() {
    }

    public static Observation collect(NPCEntity npc) {
        return new Observation(npc.level().getGameTime(), npc.getSensorState().toObservationText());
    }
}
