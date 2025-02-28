package team.jackdaw.npcsystem.ai.master;

import team.jackdaw.npcsystem.ai.ConversationWindow;

public class MasterCW extends ConversationWindow {
    MasterCW() {
        super(Master.getMaster().getUUID());
    }

    @Override
    public boolean discard() {
        return false;
    }
}
