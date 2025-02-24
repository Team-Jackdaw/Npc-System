package team.jackdaw.npcsystem.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.Optional;

public class NPCTaskListProvider extends VillagerTaskListProvider {

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> creatDoNoThingTasks(VillagerProfession profession, float speed) {
        return ImmutableList.of(createFreeFollowTask());
    }

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createCoreTasks(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                Pair.of(0, new StayAboveWaterTask(0.8F)),
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
                new Pair[]{
                        Pair.of(5, WalkToNearestVisibleWantedItemTask.create(speed, false, 4)),
                        Pair.of(6, FindPointOfInterestTask.create(profession.acquirableWorkstation(), MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, true, Optional.empty())),
                        Pair.of(7, new WalkTowardJobSiteTask(speed)),
                        Pair.of(8, TakeJobSiteTask.create(speed)),
                        Pair.of(10, FindPointOfInterestTask.create((poiType) -> poiType.matchesKey(PointOfInterestTypes.HOME), MemoryModuleType.HOME, false, Optional.of((byte) 14))),
                        Pair.of(10, FindPointOfInterestTask.create((poiType) -> poiType.matchesKey(PointOfInterestTypes.MEETING), MemoryModuleType.MEETING_POINT, true, Optional.of((byte) 14))),
                        Pair.of(10, GoToWorkTask.create()),
                        Pair.of(10, LoseJobOnSiteLossTask.create())
                });
    }

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createIdleTasks(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                Pair.of(2,
                        new RandomTask(ImmutableList.of(
                                Pair.of(FindEntityTask.create(EntityType.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 2),
                                Pair.of(FindEntityTask.create(EntityType.VILLAGER, 8, PassiveEntity::isReadyToBreed, PassiveEntity::isReadyToBreed, MemoryModuleType.BREED_TARGET, speed, 2), 1),
                                Pair.of(FindEntityTask.create(EntityType.CAT, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 1),
                                Pair.of(FindWalkTargetTask.create(speed), 1),
                                Pair.of(GoTowardsLookTargetTask.create(speed, 2), 1),
                                Pair.of(new JumpInBedTask(speed), 1),
                                Pair.of(new WaitTask(30, 60), 1))
                        )
                ),
                Pair.of(3, new GiveGiftsToHeroTask(100)),
                Pair.of(3, FindInteractionTargetTask.create(EntityType.PLAYER, 4)),
                Pair.of(3, new HoldTradeOffersTask(400, 1600)),
                Pair.of(3, new CompositeTask(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), CompositeTask.Order.ORDERED, CompositeTask.RunMode.RUN_ONE, ImmutableList.of(Pair.of(new GatherItemsVillagerTask(), 1)))),
                Pair.of(3, new CompositeTask(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.BREED_TARGET), CompositeTask.Order.ORDERED, CompositeTask.RunMode.RUN_ONE, ImmutableList.of(Pair.of(new VillagerBreedTask(), 1)))), createFreeFollowTask(),
                Pair.of(99, ScheduleActivityTask.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createWorkTasks(VillagerProfession profession, float speed) {
        Object villagerWorkTask;
        if (profession == VillagerProfession.FARMER) {
            villagerWorkTask = new FarmerWorkTask();
        } else {
            villagerWorkTask = new VillagerWorkTask();
        }

        return ImmutableList.of(
                createBusyFollowTask(),
                Pair.of(5, new RandomTask(ImmutableList.of(
                        Pair.of(villagerWorkTask, 7),
                        Pair.of(GoToIfNearbyTask.create(MemoryModuleType.JOB_SITE, 0.4F, 4), 2),
                        Pair.of(GoToNearbyPositionTask.create(MemoryModuleType.JOB_SITE, 0.4F, 1, 10), 5),
                        Pair.of(GoToSecondaryPositionTask.create(MemoryModuleType.SECONDARY_JOB_SITE, speed, 1, 6, MemoryModuleType.JOB_SITE), 5),
                        Pair.of(new FarmerVillagerTask(), profession == VillagerProfession.FARMER ? 2 : 5),
                        Pair.of(new BoneMealTask(), profession == VillagerProfession.FARMER ? 4 : 7)
                ))),
                Pair.of(10, new HoldTradeOffersTask(400, 1600)),
                Pair.of(10, FindInteractionTargetTask.create(EntityType.PLAYER, 4)),
                Pair.of(2, VillagerWalkTowardsTask.create(MemoryModuleType.JOB_SITE, speed, 9, 100, 1200)),
                Pair.of(3, new GiveGiftsToHeroTask(100)),
                Pair.of(99, ScheduleActivityTask.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createRestTasks(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                Pair.of(2, VillagerWalkTowardsTask.create(MemoryModuleType.HOME, speed, 1, 150, 1200)),
                Pair.of(3, ForgetCompletedPointOfInterestTask.create((poiType) -> poiType.matchesKey(PointOfInterestTypes.HOME), MemoryModuleType.HOME)),
                Pair.of(3, new SleepTask()),
                Pair.of(5, new RandomTask(ImmutableMap.of(MemoryModuleType.HOME, MemoryModuleState.VALUE_ABSENT), ImmutableList.of(
                        Pair.of(WalkHomeTask.create(speed), 1),
                        Pair.of(WanderIndoorsTask.create(speed), 4),
                        Pair.of(GoToPointOfInterestTask.create(speed, 4), 2),
                        Pair.of(new WaitTask(20, 40), 2)
                ))),
                createBusyFollowTask(),
                Pair.of(99, ScheduleActivityTask.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createSocialTasks(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                Pair.of(2, Tasks.pickRandomly(ImmutableList.of(
                                Pair.of(GoToIfNearbyTask.create(MemoryModuleType.MEETING_POINT, 0.4F, 40), 2),
                                Pair.of(MeetVillagerTask.create(), 2))
                        )
                ),
                Pair.of(10, new HoldTradeOffersTask(400, 1600)),
                Pair.of(10, FindInteractionTargetTask.create(EntityType.PLAYER, 4)),
                Pair.of(2, VillagerWalkTowardsTask.create(MemoryModuleType.MEETING_POINT, speed, 6, 100, 200)),
                Pair.of(3, new GiveGiftsToHeroTask(100)),
                Pair.of(3, ForgetCompletedPointOfInterestTask.create(
                        (poiType) -> poiType.matchesKey(PointOfInterestTypes.MEETING),
                        MemoryModuleType.MEETING_POINT)),
                Pair.of(3, new CompositeTask(
                        ImmutableMap.of(),
                        ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET),
                        CompositeTask.Order.ORDERED,
                        CompositeTask.RunMode.RUN_ONE,
                        ImmutableList.of(Pair.of(new GatherItemsVillagerTask(), 1)))
                ),
                createFreeFollowTask(),
                Pair.of(99, ScheduleActivityTask.create()));
    }

    private static Pair<Integer, Task<LivingEntity>> createFreeFollowTask() {
        return Pair.of(5, new RandomTask(ImmutableList.of(
                Pair.of(LookAtMobTask.create(EntityType.CAT, 8.0F), 8),
                Pair.of(LookAtMobTask.create(EntityType.VILLAGER, 8.0F), 2),
                Pair.of(LookAtMobTask.create(EntityType.PLAYER, 8.0F), 2),
                Pair.of(LookAtMobTask.create(SpawnGroup.CREATURE, 8.0F), 1),
                Pair.of(LookAtMobTask.create(SpawnGroup.WATER_CREATURE, 8.0F), 1),
                Pair.of(LookAtMobTask.create(SpawnGroup.AXOLOTLS, 8.0F), 1),
                Pair.of(LookAtMobTask.create(SpawnGroup.UNDERGROUND_WATER_CREATURE, 8.0F), 1),
                Pair.of(LookAtMobTask.create(SpawnGroup.WATER_AMBIENT, 8.0F), 1),
                Pair.of(LookAtMobTask.create(SpawnGroup.MONSTER, 8.0F), 1),
                Pair.of(new WaitTask(30, 60), 2)
        )));
    }

    private static Pair<Integer, Task<LivingEntity>> createBusyFollowTask() {
        return Pair.of(5, new RandomTask(ImmutableList.of(
                Pair.of(LookAtMobTask.create(EntityType.VILLAGER, 8.0F), 2),
                Pair.of(LookAtMobTask.create(EntityType.PLAYER, 8.0F), 2),
                Pair.of(new WaitTask(30, 60), 8)
        )));
    }
}
