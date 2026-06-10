package team.jackdaw.npcsystem.entity.task;

import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.entity.NPCEntity;

public class SpeakTask implements NpcTask {
    private final String message;
    private boolean spoken;

    public SpeakTask(String message) {
        this.message = message;
    }

    @Override
    public String name() {
        return "speak";
    }

    @Override
    public void tick(NPCEntity npc) {
        if (!spoken) {
            npc.sendMessage(message, Config.range);
            spoken = true;
        }
    }

    @Override
    public boolean isFinished(NPCEntity npc) {
        return spoken;
    }
}
