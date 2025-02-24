package team.jackdaw.npcsystem.entity;

import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import team.jackdaw.npcsystem.ai.npc.Action;

public class NPCActivity extends Activity {
    public static final Activity SOCIAL = register("social");
    public static final Activity DONOTHING = register("donothing");

    public NPCActivity(String id) {
        super(id);
    }

    public static NPCActivity mapping(Action action) {
        return null;
    }

    private static Activity register(String id) {
        return Registry.register(Registries.ACTIVITY, id, new Activity(id));
    }
}
