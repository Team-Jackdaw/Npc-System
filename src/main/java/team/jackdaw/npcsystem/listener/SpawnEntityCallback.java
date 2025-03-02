package team.jackdaw.npcsystem.listener;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.util.ActionResult;

public interface SpawnEntityCallback {
    Event<SpawnEntityCallback> EVENT = EventFactory.createArrayBacked(SpawnEntityCallback.class,
            (listeners) -> (entity) -> {
                for (SpawnEntityCallback listener : listeners) {
                    ActionResult result = listener.interact(entity);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }

                return ActionResult.PASS;
            });

    ActionResult interact(Entity entity);
}
