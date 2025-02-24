package team.jackdaw.npcsystem.entity;

import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.ai.brain.ScheduleBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.ai.npc.Action;

import java.util.Map;

public class NPCSchedule extends Schedule {

    public static final Schedule NPC_DEFAULT;
    public static Schedule get(NPC ai) {
        String id = "npc_" + ai.getUUID().toString();
        Map<Integer, Action> schedule = ai.getSchedule();
        ScheduleBuilder scheduleBuilder = getOrRegister(id);
        for (int time : schedule.keySet()) {
            NPCActivity activity = NPCActivity.mapping(schedule.get(time));
            scheduleBuilder.withActivity(time, activity);
        }
        return scheduleBuilder.build();
    }

    protected static ScheduleBuilder getOrRegister(String id) {
        Schedule schedule = Registries.SCHEDULE.get(new Identifier(id));
        if (schedule == null) {
            schedule = new Schedule();
        }
        return new ScheduleBuilder(schedule);
    }

    static {
        NPC_DEFAULT = register("npc_default").withActivity(0, NPCActivity.DONOTHING).build();
    }
}
