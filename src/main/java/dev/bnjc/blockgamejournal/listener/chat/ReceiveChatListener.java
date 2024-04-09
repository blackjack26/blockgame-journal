package dev.bnjc.blockgamejournal.listener.chat;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

public interface ReceiveChatListener {
  Event<ReceiveChatListener> EVENT = EventFactory.createArrayBacked(ReceiveChatListener.class, (listeners) -> (client, message) -> {
    for (ReceiveChatListener listener : listeners) {
      ActionResult result = listener.receiveChatMessage(client, message);

      if(result != ActionResult.PASS) {
        return result;
      }
    }

    return ActionResult.PASS;
  });

  ActionResult receiveChatMessage(MinecraftClient client, String message);
}
