package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.gui.screen.JournalScreen;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.TrackingList;
import dev.bnjc.blockgamejournal.journal.recipe.JournalPlayerInventory;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import dev.bnjc.blockgamejournal.util.Profession;
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

public class TrackingWidget extends ScrollableViewWidget {
  private static Set<String> expandedIngredients = new HashSet<>();
  private static boolean flattened = false;

  private final Screen parent;
  private final TextRenderer textRenderer;

  private JournalPlayerInventory inventory;
  private TrackingList trackingList;

  private Map<String, Integer[]> ingredientPositions;
  private Map<JournalEntry, Integer[]> itemPositions;
  private int flattenY;

  private int lastY;
  private int xOffset = 10;

  public TrackingWidget(Screen parent, int x, int y, int width, int height) {
    super(x, y, width, height, Text.empty());

    this.parent = parent;
    this.textRenderer = MinecraftClient.getInstance().textRenderer;

    this.lastY = y + 12;
    this.inventory = JournalPlayerInventory.defaultInventory();
    this.ingredientPositions = new HashMap<>();
    this.itemPositions = new HashMap<>();
  }

  public void setEntries(List<JournalEntry> entries) {
    this.trackingList = new TrackingList(entries);
    if (flattened) {
      this.trackingList.flatten();
    } else {
      this.trackingList.nest();
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
    if (!this.visible || trackingList == null || trackingList.getEntries().isEmpty()) {
      return;
    }

    this.lastY = this.getY() + 12;
    this.xOffset = 10;
    this.ingredientPositions.clear();
    this.itemPositions.clear();

    // Render "Tracking List" text
    context.drawText(textRenderer, Text.literal("Recipe Tracking List").formatted(Formatting.WHITE, Formatting.BOLD), this.getX() + xOffset, this.lastY, 0x404040, false);
    this.lastY += 12;

    for (JournalEntry entry : this.trackingList.getEntries()) {
      this.renderEntryHeader(context, this.getX() + xOffset, this.lastY, entry);
      this.lastY += 16;
    }
    this.lastY += 12; // Add some spacing

    Text reqText = Text.literal("Requirements").formatted(Formatting.WHITE, Formatting.BOLD);
    context.drawText(textRenderer, reqText, this.getX() + xOffset, this.lastY, 0x404040, false);

    Text flattenText = Text.literal(flattened ? "Unflatten" : "Flatten").formatted(Formatting.DARK_PURPLE, Formatting.UNDERLINE);
    context.drawText(
        textRenderer,
        flattenText,
        this.getX() + xOffset + textRenderer.getWidth(reqText) + 12,
        this.lastY,
        0x404040,
        false
    );
    this.flattenY = this.lastY;

    this.lastY += 12;

    this.renderCost(context, trackingList.getCost());
    this.renderRecipesKnown(context, trackingList.getKnownRecipes());
    this.renderRequiredProfessions(context, trackingList.getProfessions());
    this.renderIngredients(context, trackingList.getIngredientItems());

    this.renderTooltip(context, mouseX, mouseY);
  }

  private void renderEntryHeader(DrawContext context, int x, int y, JournalEntry entry) {
    ItemStack item = entry.getItem();
    if (item == null) {
      return;
    }

    context.drawItem(item, x, y);

    if (entry.getCount() > 1) {
      context.getMatrices().push();
      context.getMatrices().translate(0, 0, 200.0f);
      context.drawText(textRenderer, Text.literal("" + entry.getCount()).formatted(Formatting.WHITE), x + 8, y + 8, 0x404040, true);
      context.getMatrices().pop();
    }

    context.drawText(textRenderer, ItemUtil.getName(item), x + 24, y + 6, 0xFFFFFF, false);

    this.itemPositions.put(entry, new Integer[] { y, y + 16 });
  }

  private void renderCost(DrawContext context, float cost) {
    if (cost <= 0) {
      // No cost
      return;
    }

    int x = this.getX() + xOffset;
    context.drawItem(new ItemStack(Items.GOLD_NUGGET), x, this.lastY);

    boolean obtained = false;
    MutableText text = Text.empty();
    if (Journal.INSTANCE == null || Journal.INSTANCE.getMetadata().getPlayerBalance() == -1f) {
      text.append(Text.literal("? ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
    } else if (Journal.INSTANCE.getMetadata().getPlayerBalance() >= cost) {
      obtained = true;
      text.append(Text.literal("✔ ").formatted(Formatting.DARK_GREEN));
    } else {
      text.append(Text.literal("✖ ").formatted(Formatting.DARK_RED));
    }

    MutableText coinText = Text.literal(cost + " Coin").formatted(obtained ? Formatting.DARK_GRAY : Formatting.WHITE);
    if (obtained) {
      coinText.formatted(Formatting.STRIKETHROUGH);
    }
    text.append(coinText);

    context.drawText(textRenderer, text, x + 20, this.lastY + 4, 0x404040, false);

    this.lastY += 16;
  }

  private void renderRecipesKnown(DrawContext context, Map<String, Boolean> knownRecipes) {
    if (knownRecipes.isEmpty()) {
      // No known recipes
      return;
    }

    int x = this.getX() + xOffset;
    for (Map.Entry<String, Boolean> recipe : knownRecipes.entrySet()) {
      if (recipe.getValue() == null|| Journal.INSTANCE == null) {
        continue;
      }

      ItemStack item = Journal.INSTANCE.getKnownItem(recipe.getKey());
      if (item != null) {
        context.drawItem(new ItemStack(Items.BOOK), x, this.lastY);

        boolean obtained = recipe.getValue();
        MutableText text = Text.literal(obtained ? "✔ " : "✖ ").formatted(obtained ? Formatting.DARK_GREEN : Formatting.DARK_RED);
        MutableText recipeText = Text.literal("Recipe - " + ItemUtil.getName(item))
            .formatted(obtained ? Formatting.DARK_GRAY : Formatting.WHITE);

        if (obtained) {
          recipeText.formatted(Formatting.STRIKETHROUGH);
        }

        text.append(recipeText);

        this.lastY = GuiUtil.drawMultiLineText(context, textRenderer, x + 20, this.lastY, text, this.getWidth() - 20);
        this.lastY += 6;
      }
    }
  }

  private void renderRequiredProfessions(DrawContext context, Map<String, Integer> professions) {
    if (professions.isEmpty()) {
      return;
    }

    int x = this.getX() + xOffset;

    for (Map.Entry<String, Integer> profEntry : professions.entrySet()) {
      Profession profession = Profession.fromClass(profEntry.getKey());
      if (profession == null) {
        continue;
      }

      context.drawItem(new ItemStack(profession.getItem()), x, this.lastY);

      boolean obtained = false;
      MutableText text = Text.empty();
      if (Journal.INSTANCE == null || Journal.INSTANCE.getMetadata().getProfessionLevels().get(profEntry.getKey()) == null) {
        text.append(Text.literal("? ").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
      } else if (Journal.INSTANCE.getMetadata().getProfessionLevels().get(profEntry.getKey()) >= profEntry.getValue()) {
        obtained = true;
        text.append(Text.literal("✔ ").formatted(Formatting.DARK_GREEN));
      } else {
        text.append(Text.literal("✖ ").formatted(Formatting.DARK_RED));
      }

      MutableText profText = Text
          .literal("Requires " + profEntry.getValue() + " in " + profEntry.getKey())
          .formatted(obtained ? Formatting.DARK_GRAY : Formatting.WHITE);

      if (obtained) {
        profText.formatted(Formatting.STRIKETHROUGH);
      }

      text.append(profText);

      this.lastY = GuiUtil.drawMultiLineText(context, textRenderer, x + 20, this.lastY, text, this.getWidth() - 20);
      this.lastY += 6;
    }
  }

  private void renderIngredients(DrawContext context, List<ItemStack> ingredients) {
    this.renderIngredients(context, ingredients, null, 1);
  }

  private void renderIngredients(DrawContext context, List<ItemStack> ingredients, String parentKey, int quantity) {
    if (ingredients.isEmpty()) {
      return;
    }

    int x = this.getX() + xOffset;

    for (ItemStack item : ingredients) {
      int startY = this.lastY;
      String ingredientKey = (parentKey != null ? parentKey + ";" : "") + ItemUtil.getKey(item);

      // Render item
      context.drawItem(item, x, this.lastY);

      // Render text
      int itemCount = item.getCount() * quantity;
      int neededCount = this.inventory.neededCount(item, itemCount);
      int inventoryCount = itemCount - neededCount;
      boolean hasEnough = neededCount <= 0;

      MutableText text = Text.literal(hasEnough ? "✔ " : "✖ ").formatted(hasEnough ? Formatting.DARK_GREEN : Formatting.DARK_RED);
      MutableText itemText = Text.literal(ItemUtil.getName(item)).formatted(hasEnough ? Formatting.DARK_GRAY : Formatting.WHITE);
      if (hasEnough) {
        itemText.formatted(Formatting.STRIKETHROUGH);
      } else if (Journal.INSTANCE.hasJournalEntry(item)) {
        itemText.formatted(Formatting.UNDERLINE);
      }
      text.append(itemText);

      if (itemCount > 1) {
        MutableText countText = Text.literal(" (");

        countText.append(Text.literal("" + inventoryCount)
                .formatted(hasEnough ? Formatting.DARK_GREEN : Formatting.DARK_RED));

        countText.append(Text.literal("/" + itemCount + ")"));
        countText.setStyle(countText.getStyle().withColor(0x8A8A8A));
        text.append(countText);
      }

      this.lastY = GuiUtil.drawMultiLineText(context, this.textRenderer, x + 20, this.lastY, text, this.getWidth() - 20);
      this.lastY += 6;

      int endY = this.lastY;
      this.ingredientPositions.put(ingredientKey, new Integer[] { startY, endY });

      if (expandedIngredients.contains(ingredientKey)) {
        xOffset += 16;

        // TODO: Render stuff
        List<JournalEntry> entries = Journal.INSTANCE.getEntries().get(ItemUtil.getKey(item));
        if (entries != null && !entries.isEmpty()) {
          JournalEntry nextEntry = entries.get(0);
          int nextCount = nextEntry.getCount();
          int reqCount = itemCount - inventoryCount; // Remove the count that is already in the inventory
          int quantityNeeded = (int) Math.ceil((double) reqCount / nextCount);

          this.renderCost(context, nextEntry.getCost() * quantityNeeded);

          Boolean recipeKnown = nextEntry.recipeKnown();
          if (recipeKnown != null) {
            this.renderRecipesKnown(context, Map.of(nextEntry.getKey(), recipeKnown));
          }
          this.renderRequiredProfessions(context, Map.of(nextEntry.getRequiredClass(), nextEntry.getRequiredLevel()));
          this.renderIngredients(context, nextEntry.getIngredientItems(), ingredientKey, quantityNeeded);
        }

        xOffset -= 16;
        lastY += 2;
      }
    }
  }

  private void renderTooltip(DrawContext context, int mouseX, int mouseY) {
    if (!this.isHovered()) {
      return;
    }

    for (Map.Entry<JournalEntry, Integer[]> entry : this.itemPositions.entrySet()) {
      Integer[] bounds = entry.getValue();
      if (mouseY + getScrollY() >= bounds[0] && mouseY + getScrollY() <= bounds[1]) {
        JournalEntry journalEntry = entry.getKey();
        if (journalEntry != null && journalEntry.getItem() != null) {
          List<Text> tooltipText = new ArrayList<>();

          tooltipText.add(Text.literal(ItemUtil.getName(journalEntry.getItem())).formatted(Formatting.WHITE));

          if (journalEntry.getNpcName() != null) {
            tooltipText.add(Text.literal("Vendor: ").formatted(Formatting.GRAY)
                .append(Text.literal(journalEntry.getNpcName()).formatted(Formatting.DARK_AQUA)));
          }

          tooltipText.add(Text.empty());
          tooltipText.add(Text.literal("Right-click to remove from list").formatted(Formatting.ITALIC, Formatting.GRAY));

          context.getMatrices().push();
          context.getMatrices().translate(0, 0, 200.0f);
          context.drawTooltip(textRenderer, tooltipText, mouseX, mouseY);
          context.getMatrices().pop();

          return;
        }
      }
    }

    for (Map.Entry<String, Integer[]> entry : this.ingredientPositions.entrySet()) {
      Integer[] bounds = entry.getValue();

      String[] keys = entry.getKey().split(";");
      String clickedItemKey = keys[keys.length - 1];

      if (mouseY + getScrollY() >= bounds[0] && mouseY + getScrollY() <= bounds[1]) {
        ItemStack stack = Journal.INSTANCE.getKnownItem(clickedItemKey);
        if (stack != null) {
          List<Text> tooltipText = new ArrayList<>();
          tooltipText.add(Text.literal(ItemUtil.getName(stack)).formatted(Formatting.WHITE));

          JournalEntry journalEntry = Journal.INSTANCE.getFirstJournalEntry(clickedItemKey);
          if (journalEntry != null) {
            tooltipText.add(Text.literal("Vendor: ").formatted(Formatting.GRAY)
                .append(Text.literal(journalEntry.getNpcName()).formatted(Formatting.DARK_AQUA)));
            tooltipText.add(Text.empty());
            tooltipText.add(Text.literal("Left-click to expand/collapse").formatted(Formatting.ITALIC, Formatting.GRAY));
          }

          context.getMatrices().push();
          context.getMatrices().translate(0, 0, 200.0f);
          context.drawTooltip(textRenderer, tooltipText, mouseX, mouseY);
          context.getMatrices().pop();

          return;
        }
      }
    }
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {

  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (!this.active || !this.visible || Journal.INSTANCE == null) {
      return false;
    }

    if (this.isWithinBounds(mouseX, mouseY)) {
      if (button == 0) {
        for (Map.Entry<String, Integer[]> entry : this.ingredientPositions.entrySet()) {
          Integer[] bounds = entry.getValue();
          if (mouseY + getScrollY() >= bounds[0] && mouseY + getScrollY() <= bounds[1]) {
            String itemKey = entry.getKey();

            // Split the key on ";" to get all parent keys
            String[] keys = itemKey.split(";");

            // Last key is the item key
            String clickedItemKey = keys[keys.length - 1];
            if (Journal.INSTANCE.hasJournalEntry(clickedItemKey)) {
              this.playDownSound(MinecraftClient.getInstance().getSoundManager());

              if (expandedIngredients.contains(itemKey)) {
                expandedIngredients.remove(itemKey);
              } else {
                expandedIngredients.add(itemKey);
              }

              return true;
            }
          }
        }

        if (mouseY + getScrollY() >= flattenY && mouseY + getScrollY() <= flattenY + 12) {
          this.playDownSound(MinecraftClient.getInstance().getSoundManager());
          toggleFlattened();
          return true;
        }
      }
      else if (button == 1) {
        for (Map.Entry<JournalEntry, Integer[]> entry : this.itemPositions.entrySet()) {
          Integer[] bounds = entry.getValue();
          if (mouseY + getScrollY() >= bounds[0] && mouseY + getScrollY() <= bounds[1]) {
            if (entry.getKey() != null) {
              this.playDownSound(MinecraftClient.getInstance().getSoundManager());
              entry.getKey().setTracked(false);

              if (this.parent instanceof JournalScreen journalScreen) {
                journalScreen.refreshTracking();
              }

              return true;
            }
          }
        }
      }
    }

    return false;
  }

  private void toggleFlattened() {
    flattened = !flattened;
    if (flattened) {
      trackingList.flatten();
    } else {
      trackingList.nest();
    }
  }
}
