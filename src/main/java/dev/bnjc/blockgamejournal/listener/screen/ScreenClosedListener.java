package dev.bnjc.blockgamejournal.listener.screen;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.util.ActionResult;

public interface ScreenClosedListener {
  Event<ScreenClosedListener> EVENT = EventFactory.createArrayBacked(ScreenClosedListener.class, (listeners) -> (CloseHandledScreenC2SPacket p) -> {
    for (ScreenClosedListener listener : listeners) {
      ActionResult x = listener.screenClosed(p);
      if (x != ActionResult.PASS) return x;
    }

    return ActionResult.PASS;
  });

  ActionResult screenClosed(CloseHandledScreenC2SPacket packet);
}
