package dev.bnjc.blockgamejournal.listener.screen;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.util.ActionResult;

public interface ScreenSlotUpdateListener {
  Event<ScreenSlotUpdateListener> EVENT = EventFactory.createArrayBacked(ScreenSlotUpdateListener.class, (listeners) -> (packet) -> {
    for (ScreenSlotUpdateListener listener : listeners) {
      ActionResult x = listener.screenSlotUpdate(packet);
      if (x != ActionResult.PASS) return x;
    }
    return ActionResult.PASS;
  });

  ActionResult screenSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet);
}
