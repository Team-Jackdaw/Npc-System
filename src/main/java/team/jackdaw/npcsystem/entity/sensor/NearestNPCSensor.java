package team.jackdaw.npcsystem.entity.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.server.world.ServerWorld;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.Set;

public class NearestNPCSensor extends Sensor<LivingEntity> {
    public NearestNPCSensor() {
        super(20);
    }

    @Override
    protected void sense(ServerWorld world, LivingEntity entity) {
        NPCEntity npc = (NPCEntity) entity;
        entity.getBrain().remember(NPCRegistration.MEMORY_NEAREST_NPC, npc.getNearestNPCs());
    }

    @Override
    public Set<MemoryModuleType<?>> getOutputMemoryModules() {
        return ImmutableSet.of(NPCRegistration.MEMORY_NEAREST_NPC);
    }
}
