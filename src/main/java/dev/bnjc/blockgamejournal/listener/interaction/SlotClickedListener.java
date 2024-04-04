package dev.bnjc.blockgamejournal.listener.interaction;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;

public interface SlotClickedListener {
  Event<SlotClickedListener> EVENT = EventFactory.createArrayBacked(SlotClickedListener.class, (listeners) -> (syncId, slotId, button, actionType, player) -> {
    for (SlotClickedListener listener : listeners) {
      ActionResult x = listener.clickSlot(syncId, slotId, button, actionType, player);
      if (x != ActionResult.PASS) return x;
    }
    return ActionResult.PASS;
  });

  ActionResult clickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player);
}
