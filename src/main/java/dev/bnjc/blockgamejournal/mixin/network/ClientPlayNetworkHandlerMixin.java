package dev.bnjc.blockgamejournal.mixin.network;

import dev.bnjc.blockgamejournal.listener.chat.ReceiveChatListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenOpenedListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenReceivedInventoryListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

  @Inject(method = "onOpenScreen", at = @At("HEAD"), cancellable = true)
  public void onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo info) {
    ClientPlayNetworkHandler thisHandler = (ClientPlayNetworkHandler) (Object) this;
    NetworkThreadUtils.forceMainThread(packet, thisHandler, MinecraftClient.getInstance());

    ActionResult result = ScreenOpenedListener.EVENT.invoker().screenOpened(packet);

    if (result == ActionResult.SUCCESS) {
      // We have handled the screen opening, so we should cancel the rest of the method
      info.cancel();
    }
    else if (result != ActionResult.PASS) {
      // Send a packet to the server saying we have closed the window, although we never opened it
      CloseHandledScreenC2SPacket pak = new CloseHandledScreenC2SPacket(packet.getSyncId());
      thisHandler.sendPacket(pak);

      info.cancel();
    }
  }

  @Inject(method = "onInventory", at = @At("HEAD"), cancellable = true)
  public void onInventory(InventoryS2CPacket packet, CallbackInfo info) {
    ClientPlayNetworkHandler thisHandler = (ClientPlayNetworkHandler) (Object) this;
    NetworkThreadUtils.forceMainThread(packet, thisHandler, MinecraftClient.getInstance());

    ActionResult result = ScreenReceivedInventoryListener.EVENT.invoker().screenReceivedInventory(packet);
    if (result != ActionResult.PASS) {
      info.cancel();
    }
  }

  @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
  public void onGameMessage(GameMessageS2CPacket packet, CallbackInfo info) {
    ActionResult result = ReceiveChatListener.EVENT.invoker().receiveChatMessage(MinecraftClient.getInstance(), packet.content().getString());
    if (result != ActionResult.PASS) {
      info.cancel();
    }
  }
}
