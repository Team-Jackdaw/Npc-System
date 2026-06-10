package team.jackdaw.npcsystem.entity.sensor;

import java.util.Map;

public record ObservationEvent(
        ObservationType type,
        int importance,
        String text,
        long gameTime,
        Map<String, String> facts
) {
    public ObservationEvent(ObservationType type, int importance, String text, long gameTime) {
        this(type, importance, text, gameTime, Map.of());
    }
}
