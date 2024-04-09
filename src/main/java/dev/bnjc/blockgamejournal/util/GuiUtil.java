package dev.bnjc.blockgamejournal.util;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GuiUtil {
  public static Identifier sprite(String path) {
    return new Identifier(BlockgameJournal.MOD_ID, path);
  }

  public static TexturedButtonWidget close(int x, int y, ButtonWidget.PressAction pressAction) {
    TexturedButtonWidget button = new TexturedButtonWidget(
        x,
        y,
        12,
        12,
        new ButtonTextures(sprite("widgets/close/button"), sprite("widgets/close/button_highlighted")),
        pressAction
    );
    button.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.close")));
    return button;
  }
}
