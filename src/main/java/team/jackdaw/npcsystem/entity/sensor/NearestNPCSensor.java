package team.jackdaw.npcsystem.entity.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.Set;

public class NearestNPCSensor extends Sensor<VillagerEntity> {
    private static final int HORIZONTAL_EXPANSION = 16;
    private static final int HEIGHT_EXPANSION = 16;
    public NearestNPCSensor() {
        super(20);
    }

    @Override
    protected void sense(ServerWorld world, VillagerEntity entity) {
        Brain<?> brain = entity.getBrain();

//        Box box = entity.getBoundingBox().expand(HORIZONTAL_EXPANSION, HEIGHT_EXPANSION, HORIZONTAL_EXPANSION);
//        List<LivingEntity> list = world.getEntitiesByClass(LivingEntity.class, box, e -> e != entity && e.isAlive());
//        List<LivingEntity> npc_list = list.stream().filter(livingEntity -> livingEntity.getType().equals(NPCRegistration.ENTITY_NPC)).sorted(Comparator.comparingDouble(entity::squaredDistanceTo)).toList();
//
//        brain.remember(NPCRegistration.MEMORY_NEAREST_NPC, npc_list);
//        List<LivingEntity> list2 = npc_list.stream().filter((npc) -> testTargetPredicate(entity, npc)).toList();
//        brain.remember(NPCRegistration.MEMORY_NEAREST_VISIBLE_NPC, list2.isEmpty() ? null : list2.get(0));
//        Optional<LivingEntity> optional = list2.stream().filter((npc) -> testAttackableTargetPredicate(entity, npc)).findFirst();
//        brain.remember(NPCRegistration.MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC, optional);
        brain.remember(NPCRegistration.MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC, NPC_AI.getNearestNPC((NPCEntity) entity));
    }

    @Override
    public Set<MemoryModuleType<?>> getOutputMemoryModules() {
        return ImmutableSet.of(NPCRegistration.MEMORY_NEAREST_NPC, NPCRegistration.MEMORY_NEAREST_VISIBLE_NPC, NPCRegistration.MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC);
    }
}
