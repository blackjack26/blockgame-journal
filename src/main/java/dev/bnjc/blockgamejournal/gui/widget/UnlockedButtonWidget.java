package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.util.GuiUtil;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

@Setter
@Environment(value = EnvType.CLIENT)
public class UnlockedButtonWidget extends ButtonWidget {
  private Consumer<UnlockedButtonWidget> pressHandler;

  @Getter
  private static boolean toggled = false;

  public UnlockedButtonWidget(int x, int y, Consumer<UnlockedButtonWidget> pressHandler) {
    super(x, y, 12, 12, Text.empty(), null, DEFAULT_NARRATION_SUPPLIER);

    this.pressHandler = pressHandler;
    this.updateTooltip();
  }

  @Override
  public void onPress() {
    toggled = !toggled;
    this.updateTooltip();

    if (this.pressHandler != null) {
      this.pressHandler.accept(this);
    }
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    Icon icon = toggled ? (this.isHovered() ? Icon.ACTIVE_HOVER : Icon.ACTIVE) : (this.isHovered() ? Icon.INACTIVE_HOVER : Icon.INACTIVE);
    context.drawGuiTexture(icon.texture, this.getX(), this.getY(), this.width, this.height);
  }

  private void updateTooltip() {
    this.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.filter.unlocked." + (toggled ? "true" : "false"))));
  }

  @Environment(value = EnvType.CLIENT)
  enum Icon {
    ACTIVE(GuiUtil.sprite("widgets/unlocked/active_button")),
    ACTIVE_HOVER(GuiUtil.sprite("widgets/unlocked/active_button_highlighted")),
    INACTIVE(GuiUtil.sprite("widgets/unlocked/inactive_button")),
    INACTIVE_HOVER(GuiUtil.sprite("widgets/unlocked/inactive_button_highlighted"));

    final Identifier texture;

    Icon(Identifier texture) {
      this.texture = texture;
    }
  }
}
