package team.jackdaw.npcsystem.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import team.jackdaw.npcsystem.entity.task.MeetNPCTask;
import team.jackdaw.npcsystem.entity.task.MeetPlayerTask;

public class NPCTaskListProvider extends VillagerTaskListProvider {
    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createSocialTasks(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                Pair.of(2, Tasks.pickRandomly(ImmutableList.of(
                        Pair.of(GoToIfNearbyTask.create(MemoryModuleType.MEETING_POINT, 0.4f, 40), 2),
                        Pair.of(MeetVillagerTask.create(), 2)
                ))),
                Pair.of(10, new HoldTradeOffersTask(400, 1600)),
                Pair.of(10, new MeetPlayerTask()),
                Pair.of(2, new MeetNPCTask()),
                Pair.of(2, VillagerWalkTowardsTask.create(MemoryModuleType.MEETING_POINT, speed, 6, 100, 200)),
                Pair.of(3, new CompositeTask(
                        ImmutableMap.of(),
                        ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET),
                        CompositeTask.Order.ORDERED,
                        CompositeTask.RunMode.RUN_ONE,
                        ImmutableList.of(Pair.of(new GatherItemsVillagerTask(), 1))
                )),
                Pair.of(99, ScheduleActivityTask.create())
        );
    }
}
