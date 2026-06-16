package team.jackdaw.npcsystem.entity.sensor;

import team.jackdaw.npcsystem.entity.NPCEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ObservationCollector {
    private ObservationCollector() {
    }

    public static List<ObservationEvent> collect(NpcSensorState.Snapshot previous, NpcSensorState.Snapshot current, NPCEntity npc) {
        List<ObservationEvent> events = new ArrayList<>();
        collectSelfEvents(previous, current, events);
        collectWorldEvents(previous, current, events);
        collectPlayerEvents(previous, current, events);
        collectNpcEvents(previous, current, events);
        collectFunctionalBlockEvents(previous, current, events);
        collectChatEvents(previous, current, events);
        collectTaskEvents(previous, current, events);
        return events;
    }

    public static String buildSnapshotSummary(NpcSensorState.Snapshot current) {
        return current.summary();
    }

    private static void collectSelfEvents(NpcSensorState.Snapshot previous, NpcSensorState.Snapshot current, List<ObservationEvent> events) {
        if (previous.lastUpdatedGameTime() == 0L) {
            events.add(new ObservationEvent(ObservationType.STATUS_CHANGED, 3, "初次观察到自身状态。", current.lastUpdatedGameTime()));
            return;
        }
        if (Math.abs(current.health() - previous.health()) >= 0.5f) {
            int importance = current.health() < previous.health() ? 8 : 5;
            events.add(new ObservationEvent(
                    ObservationType.HEALTH_CHANGED,
                    importance,
                    "生命值从 " + previous.health() + " 变为 " + current.health() + "。",
                    current.lastUpdatedGameTime(),
                    Map.of("previous_health", String.valueOf(previous.health()), "current_health", String.valueOf(current.health()))
            ));
        }
        if (!current.position().equals(previous.position())) {
            events.add(new ObservationEvent(
                    ObservationType.STATUS_CHANGED,
                    2,
                    "位置从 " + previous.position().toShortString() + " 移动到 " + current.position().toShortString() + "。",
                    current.lastUpdatedGameTime()
            ));
        }
    }

    private static void collectWorldEvents(NpcSensorState.Snapshot previous, NpcSensorState.Snapshot current, List<ObservationEvent> events) {
        if (previous.lastUpdatedGameTime() == 0L) {
            return;
        }
        if (!current.weather().equals(previous.weather())) {
            events.add(new ObservationEvent(
                    ObservationType.WEATHER_CHANGED,
                    5,
                    "天气从 " + previous.weather() + " 变为 " + current.weather() + "。",
                    current.lastUpdatedGameTime(),
                    Map.of("previous_weather", previous.weather(), "current_weather", current.weather())
            ));
        }
    }

    private static void collectPlayerEvents(NpcSensorState.Snapshot previous, NpcSensorState.Snapshot current, List<ObservationEvent> events) {
        Map<String, NpcSensorState.EntitySummary> previousPlayers = byUuid(previous.nearbyPlayers());
        Map<String, NpcSensorState.EntitySummary> currentPlayers = byUuid(current.nearbyPlayers());
        Set<String> previousIds = previousPlayers.keySet();
        Set<String> currentIds = currentPlayers.keySet();

        for (String uuid : currentIds) {
            if (!previousIds.contains(uuid)) {
                NpcSensorState.EntitySummary player = currentPlayers.get(uuid);
                events.add(new ObservationEvent(
                        ObservationType.PLAYER_ENTERED_RANGE,
                        6,
                        "玩家 " + player.name() + " 进入了感知范围，距离约 " + formatDistance(player.distance()) + " 格。",
                        current.lastUpdatedGameTime(),
                        Map.of("player", player.name(), "uuid", uuid)
                ));
            } else if (!previousPlayers.get(uuid).lookingAtNpc() && currentPlayers.get(uuid).lookingAtNpc()) {
                NpcSensorState.EntitySummary player = currentPlayers.get(uuid);
                events.add(new ObservationEvent(
                        ObservationType.PLAYER_LOOKING_AT_NPC,
                        5,
                        "玩家 " + player.name() + " 正在看向我。",
                        current.lastUpdatedGameTime(),
                        Map.of("player", player.name(), "uuid", uuid)
                ));
            }
        }
        for (String uuid : previousIds) {
            if (!currentIds.contains(uuid)) {
                NpcSensorState.EntitySummary player = previousPlayers.get(uuid);
                events.add(new ObservationEvent(
                        ObservationType.PLAYER_LEFT_RANGE,
                        4,
                        "玩家 " + player.name() + " 离开了感知范围。",
                        current.lastUpdatedGameTime(),
                        Map.of("player", player.name(), "uuid", uuid)
                ));
            }
        }
    }

    private static void collectNpcEvents(NpcSensorState.Snapshot previous, NpcSensorState.Snapshot current, List<ObservationEvent> events) {
        Map<String, NpcSensorState.EntitySummary> previousNpcs = byUuid(previous.nearbyNpcs());
        Map<String, NpcSensorState.EntitySummary> currentNpcs = byUuid(current.nearbyNpcs());
        for (String uuid : currentNpcs.keySet()) {
            if (!previousNpcs.containsKey(uuid)) {
                NpcSensorState.EntitySummary npc = currentNpcs.get(uuid);
                events.add(new ObservationEvent(
                        ObservationType.NPC_ENTERED_RANGE,
                        4,
                        "NPC " + npc.name() + " 进入了感知范围，距离约 " + formatDistance(npc.distance()) + " 格。",
                        current.lastUpdatedGameTime(),
                        Map.of("npc", npc.name(), "uuid", uuid)
                ));
            }
        }
        for (String uuid : previousNpcs.keySet()) {
            if (!currentNpcs.containsKey(uuid)) {
                NpcSensorState.EntitySummary npc = previousNpcs.get(uuid);
                events.add(new ObservationEvent(
                        ObservationType.NPC_LEFT_RANGE,
                        3,
                        "NPC " + npc.name() + " 离开了感知范围。",
                        current.lastUpdatedGameTime(),
                        Map.of("npc", npc.name(), "uuid", uuid)
                ));
            }
        }
    }

    private static void collectFunctionalBlockEvents(NpcSensorState.Snapshot previous, NpcSensorState.Snapshot current, List<ObservationEvent> events) {
        Map<String, FunctionalBlockScanner.FunctionalBlockSummary> previousBlocks = byBlockKey(previous.functionalBlocks());
        Map<String, FunctionalBlockScanner.FunctionalBlockSummary> currentBlocks = byBlockKey(current.functionalBlocks());
        for (String key : currentBlocks.keySet()) {
            if (!previousBlocks.containsKey(key)) {
                FunctionalBlockScanner.FunctionalBlockSummary block = currentBlocks.get(key);
                events.add(new ObservationEvent(
                        ObservationType.STATUS_CHANGED,
                        4,
                        "发现附近功能方块 " + block.blockId() + "，位置 " + block.pos().toShortString() + "，距离约 " + formatDistance(block.distance()) + " 格。",
                        current.lastUpdatedGameTime(),
                        Map.of("block", block.blockId(), "category", block.category(), "pos", block.pos().toShortString())
                ));
            }
        }
        for (String key : previousBlocks.keySet()) {
            if (!currentBlocks.containsKey(key)) {
                FunctionalBlockScanner.FunctionalBlockSummary block = previousBlocks.get(key);
                events.add(new ObservationEvent(
                        ObservationType.STATUS_CHANGED,
                        3,
                        "附近功能方块 " + block.blockId() + " 不再处于感知范围内。",
                        current.lastUpdatedGameTime(),
                        Map.of("block", block.blockId(), "category", block.category(), "pos", block.pos().toShortString())
                ));
            }
        }
    }

    private static void collectChatEvents(NpcSensorState.Snapshot previous, NpcSensorState.Snapshot current, List<ObservationEvent> events) {
        Set<String> previousChats = previous.heardChats().stream().map(ObservationCollector::chatKey).collect(Collectors.toSet());
        for (NpcSensorState.HeardChat chat : current.heardChats()) {
            if (!previousChats.contains(chatKey(chat))) {
                events.add(new ObservationEvent(
                        ObservationType.CHAT_HEARD,
                        7,
                        "听到 " + chat.speakerName() + " 说：“" + chat.message() + "”。",
                        current.lastUpdatedGameTime(),
                        Map.of("speaker", chat.speakerName(), "message", chat.message())
                ));
            }
        }
    }

    private static void collectTaskEvents(NpcSensorState.Snapshot previous, NpcSensorState.Snapshot current, List<ObservationEvent> events) {
        if (previous.lastUpdatedGameTime() == 0L || current.taskStatus().equals(previous.taskStatus())) {
            return;
        }
        ObservationType type = taskType(current.taskStatus());
        int importance = type == ObservationType.TASK_FAILED ? 8 : 5;
        events.add(new ObservationEvent(
                type,
                importance,
                "任务状态从 “" + previous.taskStatus() + "” 变为 “" + current.taskStatus() + "”。",
                current.lastUpdatedGameTime(),
                Map.of("previous_task_status", previous.taskStatus(), "current_task_status", current.taskStatus())
        ));
    }

    private static ObservationType taskType(String status) {
        if (status.startsWith("started") || status.startsWith("running")) {
            return ObservationType.TASK_STARTED;
        }
        if (status.startsWith("finished")) {
            return ObservationType.TASK_FINISHED;
        }
        if (status.startsWith("cancelled")) {
            return ObservationType.TASK_CANCELLED;
        }
        if (status.contains("rejected") || status.contains("failed")) {
            return ObservationType.TASK_FAILED;
        }
        return ObservationType.STATUS_CHANGED;
    }

    private static Map<String, NpcSensorState.EntitySummary> byUuid(List<NpcSensorState.EntitySummary> entities) {
        return entities.stream().collect(Collectors.toMap(NpcSensorState.EntitySummary::uuid, Function.identity(), (a, b) -> a));
    }

    private static Map<String, FunctionalBlockScanner.FunctionalBlockSummary> byBlockKey(List<FunctionalBlockScanner.FunctionalBlockSummary> blocks) {
        return blocks.stream().collect(Collectors.toMap(block -> block.blockId() + "@" + block.pos().toShortString(), Function.identity(), (a, b) -> a));
    }

    private static String chatKey(NpcSensorState.HeardChat chat) {
        return chat.speakerUuid() + "|" + chat.gameTime() + "|" + chat.message();
    }

    private static String formatDistance(double distance) {
        return String.format(java.util.Locale.ROOT, "%.1f", distance);
    }
}
