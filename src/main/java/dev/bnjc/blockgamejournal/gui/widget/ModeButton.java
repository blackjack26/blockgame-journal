package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.util.GuiUtil;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class ModeButton extends ButtonWidget {
  public static final int SIZE = 20;

  private static final ButtonTextures TEXTURE = new ButtonTextures(
      GuiUtil.sprite("widgets/mode_background/background"),
      GuiUtil.sprite("widgets/mode_background/background_highlighted")
  );

  private final ItemStack stack;

  @Setter
  private boolean highlighted = false;

  public ModeButton(ItemStack stack, int x, int y, PressAction onPress) {
    super(x, y, SIZE, SIZE, Text.empty(), onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);

    this.stack = stack;
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    context.drawGuiTexture(
        this.highlighted || this.isHovered() ? TEXTURE.enabledFocused() : TEXTURE.enabled(),
        getX(),
        getY(),
        SIZE,
        SIZE
    );
    context.drawItem(this.stack, this.getX() + 2, this.getY() + 2);
  }
}
