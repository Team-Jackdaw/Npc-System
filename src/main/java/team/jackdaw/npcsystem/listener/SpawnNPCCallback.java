package team.jackdaw.npcsystem.listener;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;
import team.jackdaw.npcsystem.entity.NPCEntity;

public interface SpawnNPCCallback {
    Event<SpawnNPCCallback> EVENT = EventFactory.createArrayBacked(SpawnNPCCallback.class,
            (listeners) -> (entity) -> {
                for (SpawnNPCCallback listener : listeners) {
                    InteractionResult result = listener.interact(entity);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            });

    InteractionResult interact(NPCEntity entity);
}
