package dev.bnjc.blockgamejournal.listener.screen;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.util.ActionResult;

public interface ScreenOpenedListener {
  Event<ScreenOpenedListener> EVENT = EventFactory.createArrayBacked(ScreenOpenedListener.class, (listeners) -> (packet) -> {
    for (ScreenOpenedListener listener : listeners) {
      ActionResult x = listener.screenOpened(packet);
      if (x != ActionResult.PASS) return x;
    }
    return ActionResult.PASS;
  });

  ActionResult screenOpened(OpenScreenS2CPacket packet);
}
