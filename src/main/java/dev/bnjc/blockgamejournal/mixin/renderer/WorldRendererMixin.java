package dev.bnjc.blockgamejournal.mixin.renderer;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.bnjc.blockgamejournal.listener.renderer.PostRenderListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
  @Shadow
  private Framebuffer translucentFramebuffer;

  @Inject(method = "render", at = @At("RETURN"))
  private void postRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline,
                          Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager,
                          Matrix4f projectionMatrix, CallbackInfo ci) {

    if (MinecraftClient.isFabulousGraphicsOrBetter()) {
      Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
      GlStateManager._glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, this.translucentFramebuffer.fbo);
      GlStateManager._glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, framebuffer.fbo);
      GlStateManager._glBlitFrameBuffer(0, 0, framebuffer.textureWidth, framebuffer.textureHeight, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight, GlConst.GL_DEPTH_BUFFER_BIT, GlConst.GL_NEAREST);
    }

    boolean drawSignForeground = !MinecraftClient.isFabulousGraphicsOrBetter();
    PostRenderListener.EVENT.invoker().postRender(tickDelta, limitTime, matrices, drawSignForeground, true);
  }

  @Inject(method = "renderLayer", at = @At("RETURN"))
  private void renderLayer(RenderLayer renderLayer, MatrixStack matrices, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, CallbackInfo info) {
    if (MinecraftClient.isFabulousGraphicsOrBetter() && MinecraftClient.getInstance().worldRenderer.getTranslucentFramebuffer() != null) {
      MinecraftClient.getInstance().worldRenderer.getTranslucentFramebuffer().beginWrite(false);
      PostRenderListener.EVENT.invoker().postRender(MinecraftClient.getInstance().getTickDelta(), 0L, matrices, true, false);
      MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
    }
  }
}
