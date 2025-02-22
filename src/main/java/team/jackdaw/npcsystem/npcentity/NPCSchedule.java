package team.jackdaw.npcsystem.npcentity;

import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.ai.brain.ScheduleBuilder;
import team.jackdaw.npcsystem.ai.NPC;
import team.jackdaw.npcsystem.ai.npc.Action;

import java.util.Map;

public class NPCSchedule extends Schedule {

    public static final Schedule NPC_DEFAULT;
    public static Schedule get(NPC ai, String id) {
        Map<Integer, Action> schedule = ai.getSchedule();
        ScheduleBuilder scheduleBuilder = register(id);
        for (int time : schedule.keySet()) {
            NPCActivity activity = NPCActivity.mapping(schedule.get(time));
            scheduleBuilder.withActivity(time, activity);
        }
        return scheduleBuilder.build();
    }

    static {
        NPC_DEFAULT = register("villager_default")
                .withActivity(10, Activity.IDLE)
                .withActivity(2000, Activity.WORK)
                .withActivity(9000, Activity.MEET)
                .withActivity(11000, Activity.IDLE)
                .withActivity(12000, Activity.REST)
                .build();
    }
}
