package team.jackdaw.npcsystem.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestTypes;
import team.jackdaw.npcsystem.entity.task.FollowChatTargetTask;
import team.jackdaw.npcsystem.entity.task.MeetNPCTask;
import team.jackdaw.npcsystem.entity.task.MeetPlayerTask;

import java.util.Optional;

public class NPCTaskListProvider extends VillagerTaskListProvider {
    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createCoreTasks(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                Pair.of(0, new StayAboveWaterTask(0.8f)),
                Pair.of(0, OpenDoorsTask.create()),
                Pair.of(0, new LookAroundTask(45, 90)),
                Pair.of(0, new PanicTask()),
                Pair.of(0, WakeUpTask.create()),
                Pair.of(0, HideWhenBellRingsTask.create()),
                Pair.of(0, StartRaidTask.create()),
                Pair.of(0, ForgetCompletedPointOfInterestTask.create(profession.heldWorkstation(), MemoryModuleType.JOB_SITE)),
                Pair.of(0, ForgetCompletedPointOfInterestTask.create(profession.acquirableWorkstation(), MemoryModuleType.POTENTIAL_JOB_SITE)),
                Pair.of(1, new WanderAroundTask()),
                Pair.of(2, WorkStationCompetitionTask.create()),
                Pair.of(3, new FollowCustomerTask(speed)),
                Pair.of(4, new FollowChatTargetTask(speed)),
                Pair.of(5, WalkToNearestVisibleWantedItemTask.create(speed, false, 4)),
                Pair.of(6, FindPointOfInterestTask.create(profession.acquirableWorkstation(), MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, true, Optional.empty())),
                Pair.of(7, new WalkTowardJobSiteTask(speed)),
                Pair.of(8, TakeJobSiteTask.create(speed)),
                Pair.of(10, FindPointOfInterestTask.create(poiType -> poiType.matchesKey(PointOfInterestTypes.HOME), MemoryModuleType.HOME, false, Optional.of((byte) 14))),
                Pair.of(10, FindPointOfInterestTask.create(poiType -> poiType.matchesKey(PointOfInterestTypes.MEETING), MemoryModuleType.MEETING_POINT, true, Optional.of((byte) 14))),
                Pair.of(10, GoToWorkTask.create()),
                Pair.of(10, LoseJobOnSiteLossTask.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createSocialTasks(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                Pair.of(2, Tasks.pickRandomly(ImmutableList.of(
                        Pair.of(GoToIfNearbyTask.create(MemoryModuleType.MEETING_POINT, 0.4f, 40), 2),
                        Pair.of(MeetVillagerTask.create(), 2)
                ))),
                Pair.of(10, new HoldTradeOffersTask(400, 1600)),
                Pair.of(10, new MeetPlayerTask()),
                Pair.of(4, FindInteractionTargetTask.create(EntityType.PLAYER, 4)),
                Pair.of(2, new MeetNPCTask()),
                Pair.of(2, VillagerWalkTowardsTask.create(MemoryModuleType.MEETING_POINT, speed, 6, 100, 200)),
                Pair.of(3, new GiveGiftsToHeroTask(100)),
                Pair.of(3, new CompositeTask(
                        ImmutableMap.of(),
                        ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET),
                        CompositeTask.Order.ORDERED,
                        CompositeTask.RunMode.RUN_ONE,
                        ImmutableList.of(Pair.of(new GatherItemsVillagerTask(), 1))
                )),
                createFreeFollowTask(),
                Pair.of(99, ScheduleActivityTask.create()));
    }

    private static Pair<Integer, Task<LivingEntity>> createFreeFollowTask() {
        return Pair.of(5, new RandomTask(ImmutableList.of(
                Pair.of(LookAtMobTask.create(EntityType.CAT, 8.0f), 8),
                Pair.of(LookAtMobTask.create(EntityType.VILLAGER, 8.0f), 2),
                Pair.of(LookAtMobTask.create(NPCRegistration.ENTITY_NPC, 8.0f), 2),
                Pair.of(LookAtMobTask.create(EntityType.PLAYER, 8.0f), 2),
                Pair.of(LookAtMobTask.create(SpawnGroup.CREATURE, 8.0f), 1),
                Pair.of(LookAtMobTask.create(SpawnGroup.WATER_CREATURE, 8.0f), 1),
                Pair.of(LookAtMobTask.create(SpawnGroup.AXOLOTLS, 8.0f), 1),
                Pair.of(LookAtMobTask.create(SpawnGroup.UNDERGROUND_WATER_CREATURE, 8.0f), 1),
                Pair.of(LookAtMobTask.create(SpawnGroup.WATER_AMBIENT, 8.0f), 1),
                Pair.of(LookAtMobTask.create(SpawnGroup.MONSTER, 8.0f), 1),
                Pair.of(new WaitTask(30, 60), 2)
        )));
    }
}
