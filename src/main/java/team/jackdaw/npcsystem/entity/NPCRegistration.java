package team.jackdaw.npcsystem.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.ai.brain.ScheduleBuilder;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import team.jackdaw.npcsystem.entity.sensor.NearestNPCSensor;

import java.util.List;
import java.util.Optional;

public class NPCRegistration {
    public static final EntityType<NPCEntity> ENTITY_NPC = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier("npcsystem", "npc"),
            FabricEntityTypeBuilder
                    .create(SpawnGroup.MISC, NPCEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .trackRangeBlocks(10)
                    .build()
    );

    public static final Activity ACTIVITY_SOCIAL = Registry.register(Registries.ACTIVITY, "social", new Activity("social"));

    public static final MemoryModuleType<List<LivingEntity>> MEMORY_NEAREST_NPC = Registry.register(
            Registries.MEMORY_MODULE_TYPE,
            new Identifier("nearest_npc"),
            new MemoryModuleType(Optional.empty())
    );
    public static final MemoryModuleType<LivingEntity> MEMORY_NEAREST_VISIBLE_NPC = Registry.register(
            Registries.MEMORY_MODULE_TYPE,
            new Identifier("nearest_visible_npc"),
            new MemoryModuleType(Optional.empty())
    );
    public static final MemoryModuleType<LivingEntity> MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC = Registry.register(
            Registries.MEMORY_MODULE_TYPE,
            new Identifier("nearest_visible_targetable_npc"),
            new MemoryModuleType(Optional.empty())
    );

    public static final SensorType<NearestNPCSensor> SENSOR_NEAREST_NPC = Registry.register(
            Registries.SENSOR_TYPE,
            new Identifier("nearest_npc"),
            new SensorType(NearestNPCSensor::new)
    );

    public static final Schedule SCHEDULE_NPC_DEFAULT =
            new ScheduleBuilder(Registry.register(Registries.SCHEDULE, "npc_default", new Schedule()))
                    .withActivity(0, ACTIVITY_SOCIAL)
                    .build();

    static {
        FabricDefaultAttributeRegistry.register(ENTITY_NPC, NPCEntity.createVillagerAttributes());
    }
}
