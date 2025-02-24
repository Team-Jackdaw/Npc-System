package team.jackdaw.npcsystem.ai.assistant;

import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.assistant.Mark;

import java.util.List;
import java.util.UUID;

public class Assistant implements Agent {
    static int markObservation(String observation) {
        return Mark.markObservation(observation);
    }

    @Override
    public UUID getUUID() {
        return null;
    }

    @Override
    public ConversationWindow getConversationWindows() {
        return null;
    }

    @Override
    public void addTool(String tool) {

    }

    @Override
    public List<String> getTools() {
        return null;
    }

    @Override
    public boolean discard() {
        return false;
    }
}
