package dev.bnjc.blockgamejournal.mixin.network;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.listener.screen.ScreenClosedListener;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
  @Inject(method = "closeHandledScreen", at = @At("HEAD"), cancellable = true)
  public void closeHandledScreen(CallbackInfo info) {
    try {
      ActionResult result = ScreenClosedListener.EVENT.invoker().screenClosed(new CloseHandledScreenC2SPacket(((ClientPlayerEntity)(Object)this).currentScreenHandler.syncId));
      if (result != ActionResult.PASS) {
        info.cancel();
      }
    } catch (Exception e) {
      BlockgameJournal.LOGGER.error("[BlockgameJournal] Failed to invoke ClientPlayerEntityMixin#closeHandledScreen", e);
    }
  }
}
