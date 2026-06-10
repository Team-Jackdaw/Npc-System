package team.jackdaw.npcsystem.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.entity.sensor.NpcSensorState;
import team.jackdaw.npcsystem.entity.sensor.ObservationEvent;
import team.jackdaw.npcsystem.entity.sensor.ObservationCollector;
import team.jackdaw.npcsystem.entity.task.NpcTaskController;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NPCEntity extends Villager {
    protected long updateTime;
    protected TextBubbleEntity textBubble;
    private final NpcSensorState sensorState = new NpcSensorState();
    private final NpcTaskController taskController = new NpcTaskController();
    private NpcSensorState.Snapshot lastSensorSnapshot = NpcSensorState.Snapshot.empty();
    private long lastSensorUpdateTick = -20L;
    private String lastObservationSummary = "";

    public NPCEntity(EntityType<? extends Villager> entityType, Level world) {
        super(entityType, world);
    }

    public void updateScheduleFromAgent() {
        // Custom villager schedules need to be rebuilt for the 26.1 brain API.
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.level().getGameTime() - lastSensorUpdateTick >= 20L) {
            lastSensorUpdateTick = this.level().getGameTime();
            NpcSensorState.Snapshot previous = lastSensorSnapshot;
            sensorState.update(this);
            NpcSensorState.Snapshot current = sensorState.snapshot();
            List<ObservationEvent> events = ObservationCollector.collect(previous, current, this);
            lastSensorSnapshot = current;
            lastObservationSummary = ObservationCollector.buildSnapshotSummary(current);
            NPC npc = NPC_AI.getAI(this);
            if (npc != null && !events.isEmpty()) {
                npc.observe(events);
            }
        }
        taskController.tick(this);
    }

    public void sendMessage(String message, double range) {
        if (this.isRemoved()) return;
        if (Config.isBubble) {
            if(this.textBubble == null || this.textBubble.isRemoved()) {
                this.textBubble = new TextBubbleEntity(this);
            }
            textBubble.setTextBackgroundColor(Config.bubbleColor);
            textBubble.setTimeLastingPerChar(Config.timeLastingPerChar);
            textBubble.update(message);
        }
        if (Config.isChatBar) {
            String npcName = Optional.ofNullable(this.getCustomName()).orElse(Component.literal("Someone")).getString();
            this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(range), player -> true)
                    .forEach(player -> player.sendSystemMessage(Component.literal("<" + npcName + "> " + message)));
        }
        this.updateTime = System.currentTimeMillis();
    }

    public List<NPCEntity> getNearestNPCs() {
        AABB box = this.getBoundingBox().inflate(Config.range, Config.range, Config.range);
        List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive());
        return list.stream()
                .filter(livingEntity -> livingEntity.getType().equals(NPCRegistration.ENTITY_NPC))
                .map(livingEntity -> (NPCEntity) livingEntity)
                .sorted(Comparator.comparingDouble(this::distanceToSqr))
                .toList();
    }

    public Optional<NPCEntity> getNearestNPC() {
        return getNearestNPCs().stream().findFirst();
    }

    public NpcSensorState getSensorState() {
        return sensorState;
    }

    public NpcTaskController getTaskController() {
        return taskController;
    }

    public String getLastObservation() {
        return lastObservationSummary;
    }

    public void hearChat(Player speaker, String message) {
        if (!this.level().equals(speaker.level()) || this.distanceToSqr(speaker) > Config.range * Config.range) {
            return;
        }
        sensorState.recordChat(speaker, message, this);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        NPC_AI.removeNPC(this.getUUID());
    }

}
