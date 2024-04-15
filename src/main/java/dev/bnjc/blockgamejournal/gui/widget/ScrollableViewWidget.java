package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.util.GuiUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public abstract class ScrollableViewWidget extends ScrollableWidget {
  private static final Identifier SCROLLER_TEXTURE = GuiUtil.sprite("scroller");

  public ScrollableViewWidget(int x, int y, int width, int height, Text text) {
    super(x, y, width, height, text);
  }

  @Override
  public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!this.visible) {
      return;
    }

    context.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
    context.getMatrices().push();
    context.getMatrices().translate(0.0, -this.getScrollY(), 0.0);
    this.renderContents(context, mouseX, mouseY, delta);
    context.getMatrices().pop();
    context.disableScissor();
    this.renderOverlay(context);
  }

  @Override
  protected void renderOverlay(DrawContext context) {
    if (this.overflows()) {
      this.drawScrollbar(context);
    }
  }

  private void drawScrollbar(DrawContext context) {
    int h = this.getScrollbarThumbHeight();
    int x = this.getX() + this.getWidth();
    int y = Math.max(this.getY(), (int)this.getScrollY() * (this.height - h) / this.getMaxScrollY() + this.getY());
    context.drawGuiTexture(SCROLLER_TEXTURE, x - 2, y, 6, h);
  }

  private int getScrollbarThumbHeight() {
    return MathHelper.clamp((int)((float)(this.height * this.height) / (float)this.getContentsHeightWithPadding()), 32, this.height);
  }

  private int getContentsHeightWithPadding() {
    return this.getContentsHeight() + 4;
  }
}
