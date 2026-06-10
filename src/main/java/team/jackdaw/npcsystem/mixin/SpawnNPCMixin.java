package team.jackdaw.npcsystem.mixin;

import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.listener.SpawnNPCCallback;

@Mixin(PersistentEntitySectionManager.class)
public abstract class SpawnNPCMixin {
    @Inject(at = @At("TAIL"), method = "addNewEntity(Lnet/minecraft/world/level/entity/EntityAccess;)Z")
    private void onSpawnEntity(EntityAccess entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof NPCEntity npcEntity) SpawnNPCCallback.EVENT.invoker().interact(npcEntity);
    }
}
