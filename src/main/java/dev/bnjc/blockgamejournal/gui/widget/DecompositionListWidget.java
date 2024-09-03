package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.journal.DecomposedJournalEntry;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.recipe.JournalPlayerInventory;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import dev.bnjc.blockgamejournal.util.Profession;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DecompositionListWidget extends ScrollableViewWidget {
  private final DecomposedJournalEntry entry;
  private final TextRenderer textRenderer;

  // TODO: This has common functionality with RecipeWidget, consider refactoring
  private JournalPlayerInventory inventory;
  private int lastY;

  public DecompositionListWidget(DecomposedJournalEntry entry, int x, int y, int width, int height) {
    super(x, y, width, height, Text.empty());

    this.entry = entry;
    this.lastY = y + 2;

    this.textRenderer = MinecraftClient.getInstance().textRenderer;
    this.inventory = JournalPlayerInventory.defaultInventory();
  }

  public void updateYInfo(int y, int height) {
    this.setY(y);
    this.setHeight(height);
    this.visible = true;
  }

  @Override
  protected int getContentsHeight() {
    return this.lastY - this.getY();
  }

  @Override
  protected double getDeltaYPerScroll() {
    return 9.0;
  }

  @Override
  protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!this.visible) {
      return;
    }

    this.lastY = this.getY() + 2;
    this.renderCost(context);
    this.renderRecipesKnown(context);
    this.renderRequiredClasses(context);
    this.renderIngredients(context);
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {

  }

  @Override
  protected void drawBox(DrawContext context) {
    // Do not draw the box
  }

  private void renderCost(DrawContext context) {
    if (this.entry.getCost() <= 0) {
      return;
    }

    int x = this.getX() + 1;
    context.drawItem(ItemUtil.getGoldItem((int) this.entry.getCost()), x, this.lastY);

    MutableText text = Text.empty();
    if (Journal.INSTANCE == null || Journal.INSTANCE.getMetadata().getPlayerBalance() == -1f) {
      text.append(Text.literal("?").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
    } else if (Journal.INSTANCE.getMetadata().getPlayerBalance() >= entry.getCost()) {
      text.append(Text.literal("✔").formatted(Formatting.DARK_GREEN));
    } else {
      text.append(Text.literal("✖").formatted(Formatting.DARK_RED));
    }
    text.append(Text.literal(" " + entry.getCost() + " Coin").formatted(Formatting.DARK_GRAY));
    context.drawText(textRenderer, text, x + 20, this.lastY + 4, 0x404040, false);

    this.lastY += 16;
  }

  private void renderRecipesKnown(DrawContext context) {
    if (this.entry.getKnownRecipes().isEmpty()) {
      return;
    }

    for (Map.Entry<String, Boolean> recipe : this.entry.getKnownRecipes().entrySet()) {
      if (Journal.INSTANCE == null) {
        continue;
      }

      int x = this.getX() + 1;
      ItemStack item = Journal.INSTANCE.getKnownItem(recipe.getKey());
      if (item != null) {
        context.drawItem(new ItemStack(Items.BOOK), x, this.lastY);

        // TODO: Get player's known recipes
        MutableText text = Text.literal(recipe.getValue() ? "✔" : "✖").formatted(recipe.getValue() ? Formatting.DARK_GREEN : Formatting.DARK_RED);
        text.append(Text.literal(" Recipe - " + ItemUtil.getName(item)).formatted(Formatting.DARK_GRAY));

        this.lastY = GuiUtil.drawMultiLineText(context, textRenderer, x + 20, this.lastY, text, this.getWidth() - 20);
        this.lastY += 6;
      }
    }
  }

  private void renderRequiredClasses(DrawContext context) {
    if (this.entry.getProfessions().isEmpty() || Journal.INSTANCE == null) {
      return;
    }

    for (Map.Entry<String, Integer> entry : this.entry.getProfessions().entrySet()) {
      if (entry.getValue() == null || entry.getValue() == -1) {
        continue;
      }

      int x = this.getX() + 1;
      context.drawItem(Profession.getIcon(entry.getKey()), x, this.lastY);

      @Nullable Integer profLevel = Journal.INSTANCE.getMetadata().getProfessionLevels().get(entry.getKey());
      MutableText text = Text.empty();
      if (profLevel == null) {
        text.append(Text.literal("?").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
      } else if (profLevel >= entry.getValue()) {
        text.append(Text.literal("✔").formatted(Formatting.DARK_GREEN));
      } else {
        text.append(Text.literal("✖").formatted(Formatting.DARK_RED));
      }
      text.append(
          Text
              .literal(" Requires " + entry.getValue() + " in " + entry.getKey())
              .formatted(Formatting.DARK_GRAY)
      );

      this.lastY = GuiUtil.drawMultiLineText(context, textRenderer, x + 20, this.lastY, text, this.getWidth() - 20);
      this.lastY += 6;
    }
  }

  private void renderIngredients(DrawContext context) {
    if (this.entry.getIngredients().isEmpty()) {
      return;
    }

    int x = this.getX() + 1;

    for (ItemStack item : entry.getIngredientItems()) {
      // Render item
      context.drawItem(item, x, this.lastY);

      // Render text
      boolean hasEnough = this.inventory.neededCount(item) <= 0;

      // TODO: Vanilla recipe display?

      MutableText text = Text.literal(hasEnough ? "✔ " : "✖ ").formatted(hasEnough ? Formatting.DARK_GREEN : Formatting.DARK_RED);
      MutableText itemText = Text.literal(ItemUtil.getName(item)).formatted(Formatting.DARK_GRAY);
      text.append(itemText);

      if (item.getCount() > 1) {
        MutableText countText = Text.literal(" x" + item.getCount());
        countText.setStyle(countText.getStyle().withColor(0x8A8A8A));
        text.append(countText);
      }

      this.lastY = GuiUtil.drawMultiLineText(context, this.textRenderer, x + 20, this.lastY, text, this.getWidth() - 20);
      this.lastY += 6;
    }
  }
}
