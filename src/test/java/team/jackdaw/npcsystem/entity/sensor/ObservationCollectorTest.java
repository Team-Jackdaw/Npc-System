package team.jackdaw.npcsystem.entity.sensor;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObservationCollectorTest {
    @Test
    public void identicalSnapshotsDoNotProduceEvents() {
        NpcSensorState.Snapshot snapshot = snapshot(
                List.of(player("player-1", "Steve", false)),
                List.of(),
                List.of(),
                "clear",
                20.0f,
                "idle",
                List.of()
        );

        assertTrue(ObservationCollector.collect(snapshot, snapshot, null).isEmpty());
    }

    @Test
    public void detectsPlayerEnteredAndLookingAtNpc() {
        NpcSensorState.Snapshot previous = snapshot(List.of(), List.of(), List.of(), "clear", 20.0f, "idle", List.of());
        NpcSensorState.Snapshot current = snapshot(List.of(player("player-1", "Steve", true)), List.of(), List.of(), "clear", 20.0f, "idle", List.of());

        List<ObservationEvent> events = ObservationCollector.collect(previous, current, null);

        assertEquals(1, events.size());
        assertEquals(ObservationType.PLAYER_ENTERED_RANGE, events.get(0).type());
    }

    @Test
    public void detectsWeatherChatAndTaskEvents() {
        NpcSensorState.Snapshot previous = snapshot(List.of(), List.of(), List.of(), "clear", 20.0f, "idle", List.of());
        NpcSensorState.Snapshot current = snapshot(
                List.of(),
                List.of(),
                List.of(),
                "raining",
                20.0f,
                "started follow_entity",
                List.of(new NpcSensorState.HeardChat("player-1", "Steve", "hello", 3.0, 11L))
        );

        List<ObservationEvent> events = ObservationCollector.collect(previous, current, null);

        assertTrue(events.stream().anyMatch(event -> event.type() == ObservationType.WEATHER_CHANGED));
        assertTrue(events.stream().anyMatch(event -> event.type() == ObservationType.CHAT_HEARD));
        assertTrue(events.stream().anyMatch(event -> event.type() == ObservationType.TASK_STARTED));
    }

    private static NpcSensorState.EntitySummary player(String uuid, String name, boolean lookingAtNpc) {
        return new NpcSensorState.EntitySummary(uuid, name, "minecraft:player", 3.0, true, lookingAtNpc);
    }

    private static NpcSensorState.Snapshot snapshot(
            List<NpcSensorState.EntitySummary> players,
            List<NpcSensorState.EntitySummary> npcs,
            List<NpcSensorState.EntitySummary> entities,
            String weather,
            float health,
            String taskStatus,
            List<NpcSensorState.HeardChat> chats
    ) {
        return new NpcSensorState.Snapshot(
                players,
                npcs,
                entities,
                chats,
                BlockPos.ZERO,
                "minecraft:overworld",
                "minecraft:plains",
                weather,
                1000L,
                health,
                20.0f,
                true,
                false,
                10L,
                taskStatus
        );
    }
}
