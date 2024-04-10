package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.journal.DecomposedJournalEntry;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntryBuilder;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import dev.bnjc.blockgamejournal.util.Profession;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DecompositionListWidget extends ScrollableWidget {
  private final DecomposedJournalEntry entry;
  private final TextRenderer textRenderer;

  // TODO: This has common functionality with RecipeWidget, consider refactoring
  private List<ItemStack> inventory;
  private int lastY;

  public DecompositionListWidget(DecomposedJournalEntry entry, int x, int y, int width, int height) {
    super(x, y, width, height, Text.empty());

    this.entry = entry;
    this.lastY = y + 2;

    this.textRenderer = MinecraftClient.getInstance().textRenderer;

    // Populate inventory
    Entity entity = MinecraftClient.getInstance().getCameraEntity();
    if (entity instanceof ClientPlayerEntity player) {
      PlayerInventory inv = player.getInventory();
      this.inventory = new ArrayList<>();
      this.inventory.addAll(inv.main);
      this.inventory.addAll(inv.armor);
      this.inventory.addAll(inv.offHand);
    }
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
    context.drawItem(new ItemStack(Items.GOLD_NUGGET), x, this.lastY);

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

    for (Map.Entry<String, Byte> recipe : this.entry.getKnownRecipes().entrySet()) {
      if (recipe.getValue() == -1 || Journal.INSTANCE == null) {
        continue;
      }

      int x = this.getX() + 1;
      ItemStack item = Journal.INSTANCE.getKnownItem(recipe.getKey());
      if (item != null) {
        context.drawItem(new ItemStack(Items.BOOK), x, this.lastY);

        // TODO: Get player's known recipes
        MutableText text = Text.literal(recipe.getValue() == 1 ? "✔" : "✖").formatted(recipe.getValue() == 1 ? Formatting.DARK_GREEN : Formatting.DARK_RED);
        text.append(Text.literal(" Recipe - " + JournalEntryBuilder.getName(item)).formatted(Formatting.DARK_GRAY));
        context.drawText(textRenderer, text, x + 20, this.lastY + 4, 0x404040, false);
        this.lastY += 16;
      }
    }
  }

  private void renderRequiredClasses(DrawContext context) {
    if (this.entry.getProfessions().isEmpty()) {
      return;
    }

    for (Map.Entry<String, Integer> entry : this.entry.getProfessions().entrySet()) {
      int x = this.getX() + 1;

      Profession profession = Profession.fromClass(entry.getKey());
      if (profession == null) {
        continue;
      }

      context.drawItem(new ItemStack(profession.getItem()), x, this.lastY);

      MutableText text = Text.empty();
      if (Journal.INSTANCE == null || Journal.INSTANCE.getMetadata().getProfessionLevels().get(entry.getKey()) == null) {
        text.append(Text.literal("?").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
      } else if (Journal.INSTANCE.getMetadata().getProfessionLevels().get(entry.getKey()) >= entry.getValue()) {
        text.append(Text.literal("✔").formatted(Formatting.DARK_GREEN));
      } else {
        text.append(Text.literal("✖").formatted(Formatting.DARK_RED));
      }
      text.append(
          Text
              .literal(" Requires " + entry.getValue() + " in " + entry.getKey())
              .formatted(Formatting.DARK_GRAY)
      );
      context.drawText(textRenderer, text, x + 20, this.lastY + 4, 0x404040, false);

      this.lastY += 16;
    }
  }

  private void renderIngredients(DrawContext context) {
    if (this.entry.getIngredients().isEmpty()) {
      return;
    }

    int x = this.getX() + 1;

    for (ItemStack item : entry.getIngredientItems()) {
      String itemKey = JournalEntryBuilder.getKey(item);

      // Render item
      context.drawItem(item, x, this.lastY);

      // Render text
      int requiredCount = this.requiredItems(item);
      boolean hasEntry = Journal.INSTANCE != null && Journal.INSTANCE.hasJournalEntry(itemKey);

      MutableText text = Text.literal(requiredCount > 0 ? "✖ " : "✔ ").formatted(requiredCount > 0 ? Formatting.DARK_RED : Formatting.DARK_GREEN);
      MutableText itemText = Text.literal(JournalEntryBuilder.getName(item)).formatted(Formatting.DARK_GRAY);
      if (hasEntry) {
        itemText.formatted(Formatting.UNDERLINE);
      }
      text.append(itemText);

      if (item.getCount() > 1) {
        text.append(Text.literal(" x" + item.getCount()).formatted(Formatting.DARK_GRAY));
      }

      List<OrderedText> lines = this.textRenderer.wrapLines(text, this.getWidth() - 20);
      for (OrderedText oText : lines) {
        context.drawText(textRenderer, oText, x + 20, this.lastY + 4, 0x404040, false);
        this.lastY += 10;
      }
      this.lastY += 6;
    }
  }

  private int requiredItems(ItemStack stack) {
    if (this.inventory == null) {
      return stack.getCount();
    }

    // Check if the inventory contains the required count
    int requiredCount = stack.getCount();
    for (ItemStack item : this.inventory) {
      if (ItemUtil.isItemEqual(item, stack)) {
        requiredCount -= item.getCount();
      }
    }

    return requiredCount;
  }
}
