package team.jackdaw.npcsystem.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import team.jackdaw.npcsystem.entity.sensor.NearestNPCSensor;

import java.util.List;
import java.util.Optional;

public class NPCRegistration {
    private static final Identifier NPC_ID = Identifier.fromNamespaceAndPath("npcsystem", "npc");
    private static final ResourceKey<EntityType<?>> NPC_KEY = ResourceKey.create(Registries.ENTITY_TYPE, NPC_ID);

    public static final EntityType<NPCEntity> ENTITY_NPC = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            NPC_ID,
            FabricEntityTypeBuilder
                    .create(MobCategory.MISC, NPCEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .trackRangeBlocks(10)
                    .build(NPC_KEY)
    );

    public static final Activity ACTIVITY_SOCIAL = Registry.register(
            BuiltInRegistries.ACTIVITY,
            Identifier.fromNamespaceAndPath("npcsystem", "social"),
            new Activity("social")
    );

    public static final MemoryModuleType<List<NPCEntity>> MEMORY_NEAREST_NPC = Registry.register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath("npcsystem", "nearest_npc"),
            new MemoryModuleType(Optional.empty())
    );

    public static final MemoryModuleType<Entity> MEMORY_CHATTING_TARGET = Registry.register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath("npcsystem", "chatting_target"),
            new MemoryModuleType(Optional.empty())
    );

    public static final MemoryModuleType<Boolean> MEMORY_IS_CHATTING = Registry.register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath("npcsystem", "is_chatting"),
            new MemoryModuleType(Optional.empty())
    );

    public static final MemoryModuleType<Long> MEMORY_LAST_CHAT_TIME = Registry.register(
            BuiltInRegistries.MEMORY_MODULE_TYPE,
            Identifier.fromNamespaceAndPath("npcsystem", "last_chat_time"),
            new MemoryModuleType(Optional.empty())
    );

    public static final SensorType<NearestNPCSensor> SENSOR_NEAREST_NPC = Registry.register(
            BuiltInRegistries.SENSOR_TYPE,
            Identifier.fromNamespaceAndPath("npcsystem", "nearest_npc"),
            new SensorType(NearestNPCSensor::new)
    );

    static {
        FabricDefaultAttributeRegistry.register(ENTITY_NPC, NPCEntity.createAttributes());
    }
}
