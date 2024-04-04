package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.util.GuiUtil;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class VerticalScrollWidget extends ClickableWidget {
  private static final Identifier BACKGROUND = GuiUtil.sprite("scroll_bar");
  private static final ButtonTextures HANDLE_TEXTURE = new ButtonTextures(
      GuiUtil.sprite("widgets/scroll_bar/handle"),
      GuiUtil.sprite("widgets/scroll_bar/handle_disabled"),
      GuiUtil.sprite("widgets/scroll_bar/handle"),
      GuiUtil.sprite("widgets/scroll_bar/handle_disabled")
  );

  private static final int HANDLE_WIDTH = 10;
  private static final int HANDLE_HEIGHT = 11;
  private static final int INSET = 1;

  public static final int BAR_WIDTH = 2 * INSET + HANDLE_WIDTH;

  private float progress = 0f;
  private boolean scrolling = false;
  private boolean disabled = false;
  @Setter
  @Nullable
  private Consumer<Float> responder = null;

  public VerticalScrollWidget(int x, int y, int height, Text message) {
    super(x, y, BAR_WIDTH, height, message);
  }

  public void setDisabled(boolean disabled) {
    if (this.disabled != disabled) {
      this.disabled = disabled;
      this.scrolling = false;
    }
  }

  public void setProgress(float progress) {
    this.progress = MathHelper.clamp(progress, 0f, 1f);
    if (this.responder != null) {
      this.responder.accept(this.progress);
    }
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (this.visible && !this.disabled && this.isWithinBounds(mouseX, mouseY) && button == 0) {
      this.scrolling = true;
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    if (this.visible && !this.disabled) {
      setProgress((float) (this.progress - verticalAmount));
    }
    return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    if (this.scrolling && button == 0) {
      double progress = (mouseY - this.getY() - INSET - HANDLE_HEIGHT / 2) / (this.getHeight() - 2 * INSET - HANDLE_HEIGHT);
      setProgress((float) progress);
      return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (button == 0) {
      this.scrolling = false;
    }
    return super.mouseReleased(mouseX, mouseY, button);
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    context.drawGuiTexture(BACKGROUND, this.getX(), this.getY(), this.width, this.height);

    int handleY = (int) ((this.height - HANDLE_HEIGHT - 2 * INSET) * this.progress);
    context.drawGuiTexture(
        disabled ? HANDLE_TEXTURE.disabled() : HANDLE_TEXTURE.enabled(),
        this.getX() + INSET,
        this.getY() + INSET + handleY,
        HANDLE_WIDTH,
        HANDLE_HEIGHT
    );
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    // Nothing
  }

  private boolean isWithinBounds(double x, double y) {
    return x >= this.getX() && x < (this.getX() + getWidth()) && y >= getY() && y < (getY() + getHeight());
  }
}
