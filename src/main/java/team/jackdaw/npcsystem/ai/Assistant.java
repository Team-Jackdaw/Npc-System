package team.jackdaw.npcsystem.ai;

import team.jackdaw.npcsystem.ai.assistant.Mark;

public class Assistant implements Agent {
    static int markObservation(String observation) {
        return Mark.markObservation(observation);
    }

    @Override
    public ConversationWindow getConversationWindows() {
        return null;
    }
}
