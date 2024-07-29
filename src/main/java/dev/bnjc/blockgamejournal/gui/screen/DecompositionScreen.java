package dev.bnjc.blockgamejournal.gui.screen;

import dev.bnjc.blockgamejournal.gui.widget.DecompositionListWidget;
import dev.bnjc.blockgamejournal.gui.widget.NPCWidget;
import dev.bnjc.blockgamejournal.journal.DecomposedJournalEntry;
import dev.bnjc.blockgamejournal.journal.JournalMode;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

public class DecompositionScreen extends Screen {
  private static final Identifier BACKGROUND_SPRITE = GuiUtil.sprite("background");

  private static final int BUTTON_SIZE = 14;
  private static final int MENU_WIDTH = 192;
  private static final int MENU_HEIGHT = 156;

  private final DecomposedJournalEntry entry;
  private final Screen parent;
  private final boolean showNpc;

  private int left = 0;
  private int top = 0;
  private int titleBottom = 0;

  private DecompositionListWidget listWidget;

  public DecompositionScreen(DecomposedJournalEntry entry, Screen parent, boolean showNpc) {
    super(Text.empty());

    this.entry = entry;
    this.parent = parent;
    this.showNpc = showNpc;
  }

  @Override
  protected void init() {
    this.left = (this.width - MENU_WIDTH) / 2;
    this.top = (this.height - MENU_HEIGHT) / 2;

    super.init();

    // List widget
    this.listWidget = new DecompositionListWidget(this.entry, this.left + 8, titleBottom + 8, MENU_WIDTH - 20, MENU_HEIGHT - (titleBottom - this.top) - 16);
    this.listWidget.visible = false;
    this.addDrawableChild(this.listWidget);

    // Close button
    this.addDrawableChild(GuiUtil.close(
        this.left + MENU_WIDTH - (3 + BUTTON_SIZE),
        this.top + 5,
        button -> this.close()
    ));

    // Previous recipe button
    if (this.parent instanceof RecipeScreen) {
      TexturedButtonWidget prevRecipeButton = new TexturedButtonWidget(
          this.left + 5,
          this.top + 5,
          12,
          12,
          new ButtonTextures(GuiUtil.sprite("widgets/paging/prev_button"), GuiUtil.sprite("widgets/paging/prev_button_highlighted")),
          button -> MinecraftClient.getInstance().setScreen(this.parent)
      );
      prevRecipeButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.back")));
      this.addDrawableChild(prevRecipeButton);
    }

    // NPC Widget
    if (this.showNpc) {
      NPCWidget npcWidget = new NPCWidget(JournalScreen.getSelectedNpc(), this.left + MENU_WIDTH + 4, this.top, 68, 74);
      this.addDrawableChild(npcWidget);
    }

    // Tracking widget
    Screen p = this.parent;
    while (p instanceof RecipeScreen rs) {
      p = rs.getParent();
    }

    if (p instanceof JournalScreen journalScreen) {
      this.addDrawableChild(journalScreen.getTrackingWidget());
    }
  }

  @Override
  public void close() {
    // Go back to the Journal screen
    Screen p = this.parent;
    while (p instanceof RecipeScreen) {
      p = ((RecipeScreen) p).getParent();
    }
    MinecraftClient.getInstance().setScreen(p);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);

    this.renderEntryItem(context);
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);

    // Background
    context.drawGuiTexture(BACKGROUND_SPRITE, this.left, this.top, MENU_WIDTH, MENU_HEIGHT);
  }

  private void renderEntryItem(DrawContext context) {
    if (this.entry == null) {
      return;
    }

    // Item Icon
    ItemStack item = this.entry.getItem();
    if (item == null) {
      return;
    }

    int x = this.left + MENU_WIDTH / 2 - 8;
    int y = this.top + 12;
    context.drawItem(item, x, y);

    // Item count (in bottom right corner of item)
    if (this.entry.getCount() > 1) {
      context.getMatrices().push();
      context.getMatrices().translate(0.0f, 0.0f, 200.0f);
      context.drawText(textRenderer, Text.literal("" + this.entry.getCount()).formatted(Formatting.WHITE), x + 8, y + 8, 0x404040, true);
      context.getMatrices().pop();
    }

    // Title
    MutableText title = Text.literal(ItemUtil.getName(item)).formatted(Formatting.BOLD, Formatting.WHITE);
    List<OrderedText> lines = this.textRenderer.wrapLines(title, MENU_WIDTH - 20);
    titleBottom = y + 18;

    for (OrderedText oText : lines) {
      int titleX = this.left + MENU_WIDTH / 2 - this.textRenderer.getWidth(oText) / 2;
      context.drawTextWithShadow(textRenderer, oText, titleX, titleBottom, 0xFFFFFF);
      titleBottom += 10;
    }

    // Update list widget position
    this.listWidget.updateYInfo(titleBottom + 8, MENU_HEIGHT - (titleBottom - this.top) - 16);
  }
}
