package dev.bnjc.blockgamejournal.gui.screen;

import dev.bnjc.blockgamejournal.gui.widget.KnownRecipesWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class LearnedRecipesScreen extends Screen {
  private static final int WIDTH = 250;
  private final Screen parent;

  public LearnedRecipesScreen(Screen parent) {
    super(Text.literal("Learned Recipes"));

    this.parent = parent;
  }

  @Override
  protected void init() {
    super.init();

    int widgetWidth = Math.min(WIDTH, this.width - 40);
    KnownRecipesWidget knownRecipesWidget = new KnownRecipesWidget(
        this,
        (this.width - widgetWidth) / 2,
        0,
        widgetWidth,
        this.height
    );
    this.addDrawableChild(knownRecipesWidget);
  }

  @Override
  public void close() {
    if (this.client != null) {
      this.client.setScreen(this.parent);
    }
  }
}
