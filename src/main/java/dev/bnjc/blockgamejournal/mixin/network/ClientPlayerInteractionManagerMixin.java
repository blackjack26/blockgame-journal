package dev.bnjc.blockgamejournal.mixin.network;

import dev.bnjc.blockgamejournal.listener.interaction.EntityAttackedListener;
import dev.bnjc.blockgamejournal.listener.interaction.SlotClickedListener;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
  @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
  public void clickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo info) {
    ActionResult result = SlotClickedListener.EVENT.invoker().clickSlot(syncId, slotId, button, actionType, player);
    if (result != ActionResult.PASS) {
      info.cancel();
    }
  }

  @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
  public void attackEntity(PlayerEntity player, Entity entity, CallbackInfo info) {
    ActionResult result = EntityAttackedListener.EVENT.invoker().attackEntity(player, entity);
    if (result != ActionResult.PASS) {
      info.cancel();
    }
  }
}
