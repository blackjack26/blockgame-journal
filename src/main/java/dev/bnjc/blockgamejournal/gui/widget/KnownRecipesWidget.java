package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class KnownRecipesWidget extends ScrollableViewWidget {

  private final Screen parent;
  private final TextRenderer textRenderer;
  private int lastY;

  public KnownRecipesWidget(Screen parent, int x, int y, int width, int height) {
    super(x, y, width, height, Text.empty());

    this.parent = parent;
    this.textRenderer = MinecraftClient.getInstance().textRenderer;

    this.lastY = y + 12;
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
    if (!this.visible || Journal.INSTANCE == null) {
      return;
    }

    this.lastY = this.getY() + 12;

    Map<String, Boolean> knownRecipes = Journal.INSTANCE.getMetadata().getKnownRecipes();
    if (knownRecipes.isEmpty()) {
      return;
    }

    // Render "Known Recipes" text
    MutableText titleText = Text.literal("Learned Recipes").formatted(Formatting.WHITE, Formatting.BOLD);
    context.drawText(textRenderer, titleText, this.getX(), this.lastY, 0x404040, false);

    this.lastY += 12;

    // Order known recipes by name
    Map<String, String> nameMap = new HashMap<>();
    for (String recipeId : knownRecipes.keySet()) {
      // Item
      ItemStack itemStack = Journal.INSTANCE.getKnownItem(recipeId);
      String itemName;
      if (itemStack == null) {
        // Convert key to item name "mmoitems:SANCTIFIED_BOOTS" -> "Sanctified Boots"
        // Remove "mmoitems:" and replace "_" with " "
        itemName = recipeId.substring(recipeId.indexOf(":") + 1).replace("_", " ").toLowerCase();

        // Capitalize first letter of each word
        for (int i = 0; i < itemName.length(); i++) {
          if (i == 0 || itemName.charAt(i - 1) == ' ') {
            itemName = itemName.substring(0, i) + Character.toUpperCase(itemName.charAt(i)) + itemName.substring(i + 1);
          }
        }
      } else {
        itemName = ItemUtil.getName(itemStack);
      }

      nameMap.put(recipeId, itemName);
    }

    // Get keys sorted by name
    List<String> knownRecipeIds = new ArrayList<>(nameMap.keySet());
    knownRecipeIds.sort(Comparator.comparing(nameMap::get));

    for (String recipeId : knownRecipeIds) {
      boolean known = knownRecipes.get(recipeId);

      // Item
      ItemStack itemStack = Journal.INSTANCE.getKnownItem(recipeId);
      String itemName;
      if (itemStack == null) {
        itemStack = new ItemStack(Items.BOOK);

        // Convert key to item name "mmoitems:SANCTIFIED_BOOTS" -> "Sanctified Boots"
        // Remove "mmoitems:" and replace "_" with " "
        itemName = recipeId.substring(recipeId.indexOf(":") + 1).replace("_", " ").toLowerCase();

        // Capitalize first letter of each word
        for (int i = 0; i < itemName.length(); i++) {
          if (i == 0 || itemName.charAt(i - 1) == ' ') {
            itemName = itemName.substring(0, i) + Character.toUpperCase(itemName.charAt(i)) + itemName.substring(i + 1);
          }
        }
      } else {
        itemName = ItemUtil.getName(itemStack);
      }

      // Render item
      context.drawItem(itemStack, this.getX() + 2, this.lastY);
      MutableText text = Text.literal(known ? "✔ " : "✖ ").formatted(known ? Formatting.DARK_GREEN : Formatting.DARK_RED);
      text.append(Text.literal(itemName).formatted(Formatting.WHITE));
      this.lastY = GuiUtil.drawMultiLineText(
          context,
          this.textRenderer,
          this.getX() + 20,
          this.lastY,
          text,
          this.getWidth() - 20
      );
      this.lastY += 6;
    }
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {

  }
}
