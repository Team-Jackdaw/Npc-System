package team.jackdaw.npcsystem.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.entity.NPCEntity;

@Mixin(ServerWorld.class)
public abstract class SpawnEntityMixin {
    @Inject(at = @At("HEAD"), method = "spawnEntity")
    private void onSpawnEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof NPCEntity npc) {
            NPC_AI.registerNPC(npc);
            NbtCompound nbt = npc.writeNbt(new NbtCompound());
            nbt.putBoolean("Invulnerable", true);
            npc.readNbt(nbt);
        }
    }
}
