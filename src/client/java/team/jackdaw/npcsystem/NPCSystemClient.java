package team.jackdaw.npcsystem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import team.jackdaw.npcsystem.entity.NPCRegistration;

public class NPCSystemClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        try {
            Class.forName("team.jackdaw.npcsystem.entity.NPCRegistration");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        EntityRendererRegistry.register(NPCRegistration.ENTITY_NPC, VillagerEntityRenderer::new);
    }
}
