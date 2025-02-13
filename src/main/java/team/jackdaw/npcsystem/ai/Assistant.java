package team.jackdaw.npcsystem.ai;

import team.jackdaw.npcsystem.ai.assistant.MarkObservation;

public interface Assistant {
    static int markObservation(String observation) {
        return MarkObservation.markObservation(observation);
    }
}
