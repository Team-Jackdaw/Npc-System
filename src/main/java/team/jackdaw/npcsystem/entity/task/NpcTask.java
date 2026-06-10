package team.jackdaw.npcsystem.entity.task;

import team.jackdaw.npcsystem.entity.NPCEntity;

public interface NpcTask {
    String name();

    default boolean canStart(NPCEntity npc) {
        return npc.isAlive() && !npc.isRemoved();
    }

    default void start(NPCEntity npc) {
    }

    void tick(NPCEntity npc);

    boolean isFinished(NPCEntity npc);

    default void stop(NPCEntity npc) {
    }
}
