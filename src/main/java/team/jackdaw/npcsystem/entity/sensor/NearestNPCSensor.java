package team.jackdaw.npcsystem.entity.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.server.level.ServerLevel;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.Set;

public class NearestNPCSensor extends Sensor<LivingEntity> {
    public NearestNPCSensor() {
        super(20);
    }

    @Override
    protected void doTick(ServerLevel world, LivingEntity entity) {
        NPCEntity npc = (NPCEntity) entity;
        entity.getBrain().setMemory(NPCRegistration.MEMORY_NEAREST_NPC, npc.getNearestNPCs());
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(NPCRegistration.MEMORY_NEAREST_NPC);
    }
}
