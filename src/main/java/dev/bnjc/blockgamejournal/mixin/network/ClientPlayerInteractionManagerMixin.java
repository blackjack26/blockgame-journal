package dev.bnjc.blockgamejournal.mixin.network;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.listener.interaction.EntityAttackedListener;
import dev.bnjc.blockgamejournal.listener.interaction.ItemInteractListener;
import dev.bnjc.blockgamejournal.listener.interaction.SlotClickedListener;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
  @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
  public void clickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo info) {
    try {
      ActionResult result = SlotClickedListener.EVENT.invoker().clickSlot(syncId, slotId, button, actionType, player);
      if (result != ActionResult.PASS) {
        info.cancel();
      }
    } catch (Exception e) {
      BlockgameJournal.LOGGER.error("[BlockgameJournal] Failed to invoke ClientPlayerInteractionManagerMixin#clickSlot", e);
    }
  }

  @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
  public void attackEntity(PlayerEntity player, Entity entity, CallbackInfo info) {
    try {
      ActionResult result = EntityAttackedListener.EVENT.invoker().attackEntity(player, entity);
      if (result != ActionResult.PASS) {
        info.cancel();
      }
    } catch (Exception e) {
      BlockgameJournal.LOGGER.error("[BlockgameJournal] Failed to invoke ClientPlayerInteractionManagerMixin#attackEntity", e);
    }
  }

  @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
  public void interactItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
    try {
      ActionResult result = ItemInteractListener.EVENT.invoker().interactItem(player, hand);
      if (result != ActionResult.PASS) {
        cir.setReturnValue(result);
      }
    } catch (Exception e) {
      BlockgameJournal.LOGGER.error("[BlockgameJournal] Failed to invoke ClientPlayerInteractionManagerMixin#interactItem", e);
    }
  }
}
