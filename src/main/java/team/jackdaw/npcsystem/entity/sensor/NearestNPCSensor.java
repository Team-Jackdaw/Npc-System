package team.jackdaw.npcsystem.entity.sensor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class NearestNPCSensor extends Sensor<VillagerEntity> {
    public NearestNPCSensor() {
        super(20);
    }

    @Override
    protected void sense(ServerWorld world, VillagerEntity entity) {
        Brain<?> brain = entity.getBrain();
        List<LivingEntity> list = getVisibleNPC(entity).stream().sorted(Comparator.comparingDouble(entity::squaredDistanceTo)).toList();
        brain.remember(NPCRegistration.MEMORY_NEAREST_NPC, this.getVisibleNPC(entity));
        List<LivingEntity> list2 = list.stream().filter((npc) -> testTargetPredicate(entity, npc)).toList();
        brain.remember(NPCRegistration.MEMORY_NEAREST_VISIBLE_NPC, list2.isEmpty() ? null : list2.get(0));
        Optional<LivingEntity> optional = list2.stream().filter((npc) -> testAttackableTargetPredicate(entity, npc)).findFirst();
        brain.remember(NPCRegistration.MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC, optional);
    }

    private List<LivingEntity> getVisibleNPC(LivingEntity entities) {
        return ImmutableList.copyOf(this.getVisibleMobs(entities).iterate(this::isNPC));
    }

    private boolean isNPC(LivingEntity entity) {
        return entity.getType() == EntityType.VILLAGER && entity instanceof NPCEntity;
    }

    private LivingTargetCache getVisibleMobs(LivingEntity entity) {
        return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS).orElse(LivingTargetCache.empty());
    }

    @Override
    public Set<MemoryModuleType<?>> getOutputMemoryModules() {
        return ImmutableSet.of(NPCRegistration.MEMORY_NEAREST_NPC, NPCRegistration.MEMORY_NEAREST_VISIBLE_NPC, NPCRegistration.MEMORY_NEAREST_VISIBLE_TARGETABLE_NPC);
    }
}
