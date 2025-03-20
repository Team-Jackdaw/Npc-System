package team.jackdaw.npcsystem.mixin;

import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.world.entity.EntityLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.listener.SpawnNPCCallback;

@Mixin(ServerEntityManager.class)
public abstract class SpawnNPCMixin {
    @Inject(at = @At("TAIL"), method = "addEntity(Lnet/minecraft/world/entity/EntityLike;Z)Z")
    private void onSpawnEntity(EntityLike entity, boolean existing, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof NPCEntity npcEntity) SpawnNPCCallback.EVENT.invoker().interact(npcEntity);
    }
}
