package team.jackdaw.npcsystem.mixin;

import team.jackdaw.npcsystem.listener.PlayerSendMessageCallback;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerSendMessageMixin {
    @Inject(at = @At("TAIL"), method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V", cancellable = true)
    private void onSend(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound params, CallbackInfo ci) {
        InteractionResult result = PlayerSendMessageCallback.EVENT.invoker().interact(sender, message.decoratedContent().getString());

        if (result == InteractionResult.FAIL) {
            ci.cancel();
        }
    }
}
