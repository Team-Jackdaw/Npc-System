package team.jackdaw.npcsystem.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Dynamic;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.NPC_AI;

import java.util.Map;
import java.util.function.BiPredicate;

public class NPCEntity extends VillagerEntity {
    public static final Map<MemoryModuleType<GlobalPos>, BiPredicate<NPCEntity, RegistryEntry<PointOfInterestType>>> POINTS_OF_INTEREST;
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES;
    private static final ImmutableList<SensorType<? extends Sensor<? super VillagerEntity>>> SENSORS;
    protected long updateTime;
    protected TextBubbleEntity textBubble;

    static {
        MEMORY_MODULES = ImmutableList.of(
                MemoryModuleType.HOME,
                MemoryModuleType.JOB_SITE,
                MemoryModuleType.POTENTIAL_JOB_SITE,
                MemoryModuleType.MEETING_POINT,
                MemoryModuleType.MOBS,
                MemoryModuleType.VISIBLE_MOBS,
                MemoryModuleType.VISIBLE_VILLAGER_BABIES,
                MemoryModuleType.NEAREST_PLAYERS,
                MemoryModuleType.NEAREST_VISIBLE_PLAYER,
                MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
                MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
                MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
                MemoryModuleType.WALK_TARGET,
                MemoryModuleType.LOOK_TARGET,
                MemoryModuleType.INTERACTION_TARGET,
                MemoryModuleType.BREED_TARGET,
                MemoryModuleType.PATH,
                MemoryModuleType.DOORS_TO_CLOSE,
                MemoryModuleType.NEAREST_BED,
                MemoryModuleType.HURT_BY,
                MemoryModuleType.HURT_BY_ENTITY,
                MemoryModuleType.NEAREST_HOSTILE,
                MemoryModuleType.SECONDARY_JOB_SITE,
                MemoryModuleType.HIDING_PLACE,
                MemoryModuleType.HEARD_BELL_TIME,
                MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                MemoryModuleType.LAST_SLEPT,
                MemoryModuleType.LAST_WOKEN,
                MemoryModuleType.LAST_WORKED_AT_POI,
                MemoryModuleType.GOLEM_DETECTED_RECENTLY,
                NPCRegistration.MEMORY_NEAREST_NPC,
                NPCRegistration.MEMORY_NEAREST_VISIBLE_NPC,
                NPCRegistration.MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC
        );
        SENSORS = ImmutableList.of(
                SensorType.NEAREST_LIVING_ENTITIES,
                SensorType.NEAREST_PLAYERS,
                SensorType.NEAREST_ITEMS,
                SensorType.NEAREST_BED,
                SensorType.HURT_BY,
                SensorType.VILLAGER_HOSTILES,
                SensorType.VILLAGER_BABIES,
                SensorType.SECONDARY_POIS,
                SensorType.GOLEM_DETECTED,
                NPCRegistration.SENSOR_NEAREST_NPC

        );
        POINTS_OF_INTEREST = ImmutableMap.of(
                MemoryModuleType.HOME, (villager, registryEntry) -> registryEntry.matchesKey(PointOfInterestTypes.HOME),
                MemoryModuleType.JOB_SITE, (villager, registryEntry) -> villager.getVillagerData().getProfession().heldWorkstation().test(registryEntry),
                MemoryModuleType.POTENTIAL_JOB_SITE, (villager, registryEntry) -> VillagerProfession.IS_ACQUIRABLE_JOB_SITE.test(registryEntry),
                MemoryModuleType.MEETING_POINT, (villager, registryEntry) -> registryEntry.matchesKey(PointOfInterestTypes.MEETING)
        );
    }

    public NPCEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
        NbtCompound nbt = writeNbt(new NbtCompound());
        nbt.putBoolean("Invulnerable", true);
        readNbt(nbt);
    }

    protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
        NPC_AI.getOrRegisterNPC(this);
        Brain<VillagerEntity> brain = this.createBrainProfile().deserialize(dynamic);
        this.initBrain(brain);
        return brain;
    }

    protected Brain.Profile<VillagerEntity> createBrainProfile() {
        return Brain.createProfile(MEMORY_MODULES, SENSORS);
    }

    private void initBrain(Brain<VillagerEntity> brain) {
        brain.setSchedule(NPC_AI.getSchedule(this));
        VillagerProfession npcProfession = this.getVillagerData().getProfession();
        brain.setTaskList(NPCRegistration.ACTIVITY_SOCIAL, NPCTaskListProvider.createSocialTasks(npcProfession, 0.5F));
        brain.setCoreActivities(ImmutableSet.of(NPCRegistration.ACTIVITY_SOCIAL));
        brain.setDefaultActivity(NPCRegistration.ACTIVITY_SOCIAL);
        brain.doExclusively(NPCRegistration.ACTIVITY_SOCIAL);
        brain.refreshActivities(this.world.getTimeOfDay(), this.world.getTime());
    }

    @Override
    public void reinitializeBrain(ServerWorld world) {
        Brain<VillagerEntity> brain = this.getBrain();
        brain.stopAllTasks(world, this);
        this.brain = brain.copy();
        this.initBrain(this.getBrain());
    }

    public void updateScheduleFromAgent() {
        this.brain.setSchedule(NPC_AI.getSchedule(this));
        brain.doExclusively(Activity.IDLE);
        brain.refreshActivities(this.world.getTimeOfDay(), this.world.getTime());
    }

    public void sendMessage(String message, double range) {
        if (Config.isBubble) {
            if(this.textBubble.isRemoved()) {
                this.textBubble = new TextBubbleEntity(this);
            }
            textBubble.setTextBackgroundColor(Config.bubbleColor);
            textBubble.setTimeLastingPerChar(Config.timeLastingPerChar);
            textBubble.update(message);
        }
        if (Config.isChatBar) {
            String npcName = this.getCustomName() != null ? this.getCustomName().toString() : "SomeOne";
            this.world.getEntitiesByClass(PlayerEntity.class, this.getBoundingBox().expand(range), player -> true)
                    .forEach(player -> player.sendMessage(Text.of("<" + npcName + "> " + message)));
        }
        this.updateTime = System.currentTimeMillis();
    }

}
