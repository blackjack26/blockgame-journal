package dev.bnjc.blockgamejournal.listener.interaction;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public interface ItemInteractListener {
  Event<ItemInteractListener> EVENT = EventFactory.createArrayBacked(ItemInteractListener.class, (listeners) -> (player, hand) -> {
    for (ItemInteractListener listener : listeners) {
      ActionResult x = listener.interactItem(player, hand);
      if (x != ActionResult.PASS) return x;
    }
    return ActionResult.PASS;
  });

  ActionResult interactItem(PlayerEntity player, Hand hand);
}