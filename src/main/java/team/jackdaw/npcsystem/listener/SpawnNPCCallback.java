package team.jackdaw.npcsystem.listener;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;
import team.jackdaw.npcsystem.entity.NPCEntity;

public interface SpawnNPCCallback {
    Event<SpawnNPCCallback> EVENT = EventFactory.createArrayBacked(SpawnNPCCallback.class,
            (listeners) -> (entity) -> {
                for (SpawnNPCCallback listener : listeners) {
                    ActionResult result = listener.interact(entity);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }

                return ActionResult.PASS;
            });

    ActionResult interact(NPCEntity entity);
}
