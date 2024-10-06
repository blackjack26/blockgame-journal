package dev.bnjc.blockgamejournal.mixin.gui.screen;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.listener.screen.DrawSlotListener;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
  private HandledScreenMixin() {
    super(null);
  }

  @Inject(method = "drawSlot", at = @At("TAIL"))
  private void drawSlot(DrawContext context, Slot slot, CallbackInfo info) {
    try {
      DrawSlotListener.EVENT.invoker().drawSlot(context, slot);
    } catch (Exception e) {
      BlockgameJournal.LOGGER.error("[Blockgame Journal] Failed to invoke DrawSlotListener", e);
    }
  }
}
