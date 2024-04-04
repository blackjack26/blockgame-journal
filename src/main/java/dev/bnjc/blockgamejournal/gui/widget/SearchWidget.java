package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.util.GuiUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class SearchWidget extends TextFieldWidget {
  public static final Text SEARCH_MESSAGE = Text.translatable("blockgamejournal.search_hint");

  private static final Identifier SEARCH_BAR_SPRITE = GuiUtil.sprite("search_bar");

  public SearchWidget(TextRenderer textRenderer, int x, int y, int width, int height, @Nullable TextFieldWidget copyFrom, Text text) {
    super(textRenderer, x, y, width, height, copyFrom, text);
    this.setDrawsBackground(false);
  }

  @Override
  public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    context.drawGuiTexture(SEARCH_BAR_SPRITE, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    context.getMatrices().translate(2, 2, 0);
    super.renderButton(context, mouseX, mouseY, delta);
    context.getMatrices().translate(-2, -2, 0);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (isMouseOver(mouseX, mouseY)) {
      this.setText("");
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }
}
