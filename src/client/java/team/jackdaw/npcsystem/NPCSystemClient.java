package team.jackdaw.npcsystem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import team.jackdaw.npcsystem.entity.NPCRegistration;

public class NPCSystemClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(NPCRegistration.ENTITY_NPC, VillagerEntityRenderer::new);
    }
}
