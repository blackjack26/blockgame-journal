package dev.bnjc.blockgamejournal.listener.screen;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.util.ActionResult;

public interface ScreenReceivedInventoryListener {
  Event<ScreenReceivedInventoryListener> EVENT = EventFactory.createArrayBacked(ScreenReceivedInventoryListener.class, (listeners) -> (packet) -> {
    for (ScreenReceivedInventoryListener listener : listeners) {
      ActionResult x = listener.screenReceivedInventory(packet);
      if (x != ActionResult.PASS) return x;
    }
    return ActionResult.PASS;
  });

  ActionResult screenReceivedInventory(InventoryS2CPacket packet);
}
