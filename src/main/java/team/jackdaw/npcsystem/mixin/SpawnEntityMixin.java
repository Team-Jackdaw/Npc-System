package team.jackdaw.npcsystem.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.jackdaw.npcsystem.listener.SpawnEntityCallback;

@Mixin(ServerWorld.class)
public abstract class SpawnEntityMixin {
    @Inject(at = @At("HEAD"), method = "spawnEntity")
    private void onSpawnEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        SpawnEntityCallback.EVENT.invoker().interact(entity);
    }
}
