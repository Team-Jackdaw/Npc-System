package team.jackdaw.npcsystem.npcentity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

public class NPCMemoryModuleType {
    public static final MemoryModuleType<List<LivingEntity>> NEAREST_NPC;
    public static final MemoryModuleType<LivingEntity> NEAREST_VISIBLE_NPC;
    public static final MemoryModuleType<LivingEntity> NEAREST_VISIBLE_TARGETABLE_NPC;

    static {
        NEAREST_NPC = register("nearest_npc");
        NEAREST_VISIBLE_NPC = register("nearest_visible_npc");
        NEAREST_VISIBLE_TARGETABLE_NPC = register("nearest_visible_targetable_npc");
    }

    private static <U> MemoryModuleType<U> register(String id) {
        return (MemoryModuleType) Registry.register(Registries.MEMORY_MODULE_TYPE, new Identifier(id), new MemoryModuleType(Optional.empty()));
    }
}
