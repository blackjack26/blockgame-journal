package dev.bnjc.blockgamejournal.util;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class GuiUtil {
  public static Identifier sprite(String path) {
    return new Identifier(BlockgameJournal.MOD_ID, path);
  }

  public static TexturedButtonWidget close(int x, int y, ButtonWidget.PressAction pressAction) {
    return GuiUtil.button(x, y, "widgets/close", "blockgamejournal.close", pressAction);
  }

  public static TexturedButtonWidget button(int x, int y, String path, String tooltipKey, ButtonWidget.PressAction pressAction) {
    TexturedButtonWidget button = new TexturedButtonWidget(
        x,
        y,
        12,
        12,
        new ButtonTextures(sprite(path + "/button"), sprite(path + "/button_highlighted")),
        pressAction
    );
    button.setTooltip(Tooltip.of(Text.translatable(tooltipKey)));
    return button;
  }

  public static int drawMultiLineText(DrawContext context, TextRenderer textRenderer, int x, int y, Text text, int width) {
    return drawMultiLineText(context, textRenderer, x, y, text, width, 10, 4, 0x404040, false);
  }

  /**
   * @return The last y position of the text
   */
  public static int drawMultiLineText(DrawContext context, TextRenderer textRenderer, int x, int y, Text text, int width, int lineSpacing, int yOffset, int color, boolean shadow) {
    List<OrderedText> lines = textRenderer.wrapLines(text, width);
    for (OrderedText oText : lines) {
      context.drawText(textRenderer, oText, x, y + yOffset, color, shadow);
      y += lineSpacing;
    }
    return y;
  }
}
