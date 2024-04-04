package dev.bnjc.blockgamejournal.listener.interaction;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

public interface EntityAttackedListener {
  Event<EntityAttackedListener> EVENT = EventFactory.createArrayBacked(EntityAttackedListener.class, (listeners) -> (player, entity) -> {
    for (EntityAttackedListener listener : listeners) {
      ActionResult x = listener.attackEntity(player, entity);
      if (x != ActionResult.PASS) return x;
    }
    return ActionResult.PASS;
  });

  ActionResult attackEntity(PlayerEntity player, Entity entity);
}
