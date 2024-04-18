package dev.bnjc.blockgamejournal.listener.renderer;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.util.math.MatrixStack;

public interface PostRenderListener {
  Event<PostRenderListener> EVENT = EventFactory.createArrayBacked(PostRenderListener.class, (listeners) -> (tickDelta, limitTime, matrices, withDepth, withoutDepth) -> {
    for (PostRenderListener listener : listeners) {
      listener.postRender(tickDelta, limitTime, matrices, withDepth, withoutDepth);
    }
  });

  void postRender(float tickDelta, long limitTime, MatrixStack matrices, boolean withDepth, boolean withoutDepth);
}
