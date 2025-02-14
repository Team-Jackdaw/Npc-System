package team.jackdaw.npcsystem.npcentity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import team.jackdaw.npcsystem.ai.NPC;

public class NPCEntity extends VillagerEntity {
    private NPC ai;
    public NPCEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
    }

    public NPCEntity(EntityType<? extends VillagerEntity> entityType, World world, VillagerType type) {
        super(entityType, world, type);
    }
}
