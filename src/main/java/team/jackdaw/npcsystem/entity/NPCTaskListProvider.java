package team.jackdaw.npcsystem.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.ai.brain.task.ScheduleActivityTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.brain.task.VillagerTaskListProvider;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import team.jackdaw.npcsystem.entity.task.MeetNPCTask;

public class NPCTaskListProvider extends VillagerTaskListProvider {
    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createSocialTasks(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                Pair.of(10, new MeetNPCTask()),
                Pair.of(99, ScheduleActivityTask.create())
        );
    }
}
