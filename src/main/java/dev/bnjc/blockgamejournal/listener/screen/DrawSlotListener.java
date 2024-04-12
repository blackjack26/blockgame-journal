package dev.bnjc.blockgamejournal.listener.screen;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;

public interface DrawSlotListener {
  Event<DrawSlotListener> EVENT = EventFactory.createArrayBacked(DrawSlotListener.class, (listeners) -> (context, slot) -> {
    for (DrawSlotListener listener : listeners) {
      listener.drawSlot(context, slot);
    }
  });

  void drawSlot(DrawContext context, Slot slot);
}
