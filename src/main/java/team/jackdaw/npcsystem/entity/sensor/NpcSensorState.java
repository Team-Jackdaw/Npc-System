package team.jackdaw.npcsystem.entity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class NpcSensorState {
    private static final int MAX_CHAT_RECORDS = 8;
    private static final int MAX_ENTITY_SUMMARIES = 8;

    private final ArrayDeque<HeardChat> heardChats = new ArrayDeque<>();
    private List<EntitySummary> nearbyPlayers = List.of();
    private List<EntitySummary> nearbyNpcs = List.of();
    private List<EntitySummary> nearbyEntities = List.of();
    private BlockPos position = BlockPos.ZERO;
    private String dimension = "unknown";
    private String biome = "unknown";
    private String weather = "clear";
    private long dayTime;
    private float health;
    private float maxHealth;
    private boolean onGround;
    private boolean inWater;
    private long lastUpdatedGameTime;

    public void update(NPCEntity npc) {
        Level level = npc.level();
        position = npc.blockPosition();
        dimension = level.dimension().identifier().toString();
        biome = level.getBiome(position).getRegisteredName();
        weather = level.isThundering() ? "thundering" : level.isRaining() ? "raining" : "clear";
        dayTime = level.getOverworldClockTime();
        health = npc.getHealth();
        maxHealth = npc.getMaxHealth();
        onGround = npc.onGround();
        inWater = npc.isInWater();
        lastUpdatedGameTime = level.getGameTime();

        AABB rangeBox = npc.getBoundingBox().inflate(Config.range);
        nearbyPlayers = level.getEntitiesOfClass(Player.class, rangeBox, Player::isAlive)
                .stream()
                .sorted(Comparator.comparingDouble(npc::distanceToSqr))
                .limit(MAX_ENTITY_SUMMARIES)
                .map(player -> EntitySummary.from(npc, player, playerLooksAt(player, npc)))
                .toList();

        nearbyNpcs = level.getEntitiesOfClass(LivingEntity.class, rangeBox, entity -> entity != npc && entity.isAlive() && entity.getType().equals(NPCRegistration.ENTITY_NPC))
                .stream()
                .sorted(Comparator.comparingDouble(npc::distanceToSqr))
                .limit(MAX_ENTITY_SUMMARIES)
                .map(entity -> EntitySummary.from(npc, entity, false))
                .toList();

        nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, rangeBox, entity -> entity != npc && entity.isAlive() && !entity.getType().equals(NPCRegistration.ENTITY_NPC) && !(entity instanceof Player))
                .stream()
                .sorted(Comparator.comparingDouble(npc::distanceToSqr))
                .limit(MAX_ENTITY_SUMMARIES)
                .map(entity -> EntitySummary.from(npc, entity, false))
                .toList();
    }

    public void recordChat(Player speaker, String message, NPCEntity listener) {
        heardChats.addLast(new HeardChat(
                speaker.getUUID().toString(),
                speaker.getName().getString(),
                message,
                Math.sqrt(listener.distanceToSqr(speaker)),
                listener.level().getGameTime()
        ));
        while (heardChats.size() > MAX_CHAT_RECORDS) {
            heardChats.removeFirst();
        }
    }

    public String toObservationText() {
        StringBuilder builder = new StringBuilder();
        builder.append("状态: 位置 ").append(position.toShortString())
                .append(", 维度 ").append(dimension)
                .append(", 生物群系 ").append(biome)
                .append(", 天气 ").append(weather)
                .append(", 时间 ").append(dayTime)
                .append(", 生命值 ").append(String.format(Locale.ROOT, "%.1f/%.1f", health, maxHealth))
                .append(", ").append(onGround ? "在地面" : "不在地面")
                .append(", ").append(inWater ? "在水中" : "不在水中")
                .append("。\n");
        appendEntities(builder, "附近玩家", nearbyPlayers);
        appendEntities(builder, "附近NPC", nearbyNpcs);
        appendEntities(builder, "附近实体", nearbyEntities);
        if (!heardChats.isEmpty()) {
            builder.append("最近听到的聊天: ");
            heardChats.forEach(chat -> builder.append(chat.speakerName())
                    .append("说“").append(chat.message()).append("”")
                    .append(String.format(Locale.ROOT, "(%.1f格); ", chat.distance())));
            builder.append("\n");
        }
        return builder.toString().trim();
    }

    public List<EntitySummary> nearbyPlayers() {
        return nearbyPlayers;
    }

    public List<EntitySummary> nearbyNpcs() {
        return nearbyNpcs;
    }

    public List<HeardChat> heardChats() {
        return List.copyOf(heardChats);
    }

    public long lastUpdatedGameTime() {
        return lastUpdatedGameTime;
    }

    private static void appendEntities(StringBuilder builder, String label, List<EntitySummary> entities) {
        if (entities.isEmpty()) {
            builder.append(label).append(": 无。\n");
            return;
        }
        builder.append(label).append(": ");
        entities.forEach(entity -> builder.append(entity.name())
                .append("(").append(entity.type())
                .append(", ").append(String.format(Locale.ROOT, "%.1f格", entity.distance()))
                .append(entity.lookingAtNpc() ? ", 正看向我" : "")
                .append("); "));
        builder.append("\n");
    }

    private static boolean playerLooksAt(Player player, NPCEntity npc) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 toNpc = npc.getEyePosition().subtract(player.getEyePosition()).normalize();
        return look.dot(toNpc) > 0.85 && player.hasLineOfSight(npc);
    }

    public record EntitySummary(String uuid, String name, String type, double distance, boolean visible, boolean lookingAtNpc) {
        static EntitySummary from(NPCEntity npc, Entity entity, boolean lookingAtNpc) {
            return new EntitySummary(
                    entity.getUUID().toString(),
                    entity.getName().getString(),
                    entity.getType().builtInRegistryHolder().unwrapKey()
                            .map(key -> key.identifier().toString())
                            .orElse(entity.getType().toString()),
                    Math.sqrt(npc.distanceToSqr(entity)),
                    npc.hasLineOfSight(entity),
                    lookingAtNpc
            );
        }
    }

    public record HeardChat(String speakerUuid, String speakerName, String message, double distance, long gameTime) {
    }
}
