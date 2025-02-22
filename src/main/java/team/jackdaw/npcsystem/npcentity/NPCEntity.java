package team.jackdaw.npcsystem.npcentity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Dynamic;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import team.jackdaw.npcsystem.ai.NPC;

import java.util.Map;
import java.util.function.BiPredicate;

public class NPCEntity extends VillagerEntity {
    public static final Map<MemoryModuleType<GlobalPos>, BiPredicate<NPCEntity, RegistryEntry<PointOfInterestType>>> POINTS_OF_INTEREST;
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES;
    private static final ImmutableList<SensorType<? extends Sensor<? super VillagerEntity>>> SENSORS;

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
                MemoryModuleType.GOLEM_DETECTED_RECENTLY
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
                SensorType.GOLEM_DETECTED
        );
        POINTS_OF_INTEREST = ImmutableMap.of(
                MemoryModuleType.HOME, (villager, registryEntry) -> registryEntry.matchesKey(PointOfInterestTypes.HOME),
                MemoryModuleType.JOB_SITE, (villager, registryEntry) -> villager.getVillagerData().getProfession().heldWorkstation().test(registryEntry),
                MemoryModuleType.POTENTIAL_JOB_SITE, (villager, registryEntry) -> VillagerProfession.IS_ACQUIRABLE_JOB_SITE.test(registryEntry),
                MemoryModuleType.MEETING_POINT, (villager, registryEntry) -> registryEntry.matchesKey(PointOfInterestTypes.MEETING)
        );
    }

    private NPC ai;

    public NPCEntity(EntityType<? extends NPCEntity> entityType, World world) {
        super(entityType, world);
    }

    public NPCEntity(EntityType<? extends NPCEntity> entityType, World world, VillagerType type) {
        super(entityType, world, type);
    }

    protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
        Brain<VillagerEntity> brain = this.createBrainProfile().deserialize(dynamic);
        this.initBrain(brain, NPCSchedule.get(ai, ai.getUUID().toString()));
        return brain;
    }

    protected Brain.Profile<VillagerEntity> createBrainProfile() {
        return Brain.createProfile(MEMORY_MODULES, SENSORS);
    }

    private void initBrain(Brain<VillagerEntity> brain, Schedule npcSchedule) {
        brain.setSchedule(npcSchedule);
    }
}
