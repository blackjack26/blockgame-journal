package dev.bnjc.blockgamejournal.gui.screen;

import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.JournalEntryBuilder;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecipeDisplay extends Screen {
  private static final Identifier BACKGROUND_SPRITE = GuiUtil.sprite("background");

  private static final int BUTTON_SIZE = 14;
  private static final int MENU_WIDTH = 192;
  private static final int MENU_HEIGHT = 156;
  private static final int TITLE_LEFT = 8;
  private static final int TITLE_TOP = 8;

  private final Screen parent;
  private final ItemStack stack;
  private final List<JournalEntry> entries;

  private int left = 0;
  private int top = 0;

  private int page = 0;
  private TexturedButtonWidget prevPageButton;
  private TexturedButtonWidget nextPageButton;

  private PlayerInventory inventory;

  public RecipeDisplay(ItemStack stack, Screen parent) {
    super(Text.literal(JournalEntryBuilder.getName(stack)));

    this.stack = stack;
    this.parent = parent;
    if (Journal.INSTANCE == null) {
      this.entries = Collections.emptyList();
    } else {
      this.entries = Journal.INSTANCE.getEntries().getOrDefault(JournalEntryBuilder.getKey(stack), new ArrayList<>());
    }
  }

  @Override
  protected void init() {
    this.left = (this.width - MENU_WIDTH) / 2;
    this.top = (this.height - MENU_HEIGHT) / 2;

    super.init();

    // Close button
    this.addDrawableChild(GuiUtil.close(
        this.left + MENU_WIDTH - (3 + BUTTON_SIZE),
        this.top + 5,
        button -> this.close()
    ));

    // Next page button
    this.nextPageButton = new TexturedButtonWidget(
        this.left + MENU_WIDTH - (3 + BUTTON_SIZE),
        this.top + MENU_HEIGHT - (3 + BUTTON_SIZE),
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/next/button"), GuiUtil.sprite("widgets/next/button_highlighted")),
        button -> this.goToPage(this.page + 1)
    );
    this.nextPageButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.next_page")));
    this.nextPageButton.visible = this.page < this.entries.size() - 1;
    this.addDrawableChild(this.nextPageButton);

    // Previous page button
    this.prevPageButton = new TexturedButtonWidget(
        this.left + MENU_WIDTH - 2 * (3 + BUTTON_SIZE),
        this.top + MENU_HEIGHT - (3 + BUTTON_SIZE),
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/prev/button"), GuiUtil.sprite("widgets/prev/button_highlighted")),
        button -> this.goToPage(this.page - 1)
    );
    this.prevPageButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.prev_page")));
    this.prevPageButton.visible = this.page > 0;
    this.addDrawableChild(this.prevPageButton);

    // Populate inventory
    Entity entity = MinecraftClient.getInstance().getCameraEntity();
    if (entity instanceof ClientPlayerEntity player) {
      this.inventory = player.getInventory();
    }
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);

    // Main item
    this.renderMainItem(context);

    // Entries
    this.renderEntries(context);
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);

    // Background
    context.drawGuiTexture(BACKGROUND_SPRITE, this.left, this.top, MENU_WIDTH, MENU_HEIGHT);
  }

  @Override
  public void close() {
    MinecraftClient.getInstance().setScreen(this.parent);
  }

  private void renderMainItem(DrawContext context) {
    // Render main item
    int x = this.left + MENU_WIDTH / 2 - 8;
    int y = this.top + TITLE_TOP + 2;
    context.drawItem(stack, x, y);

    // Title
    int titleX = this.left + MENU_WIDTH / 2 - textRenderer.getWidth(this.title) / 2;
    int titleY = y + 18;
    context.drawText(textRenderer, this.title, titleX, titleY, 0x404040, false);
  }

  private void renderEntries(DrawContext context) {
    JournalEntry entry = this.entries.get(this.page);

    if (entry == null) {
      // TODO: Render empty entry
      return;
    }

    // Render NPC Name
    MutableText npcText = Text.literal("Crafted by ").formatted(Formatting.DARK_AQUA, Formatting.ITALIC); // TODO: Translation
    npcText.append(Text.literal(entry.getNpcName()).formatted(Formatting.DARK_AQUA, Formatting.BOLD));

    // Center Title
    int titleX = this.left + MENU_WIDTH / 2 - textRenderer.getWidth(npcText) / 2;
    int titleY = this.top + TITLE_TOP + 32;
    context.drawText(textRenderer, npcText, titleX,  titleY, 0x404040, false);

    // Render entry
    int x = this.left + TITLE_LEFT;
    int y = titleY + 20;

    // Render Cost
    if (entry.getCost() > 0) {
      context.drawItem(new ItemStack(Items.GOLD_NUGGET), x, y);

      MutableText text = Text.empty();
      if (Journal.INSTANCE == null || Journal.INSTANCE.getMetadata().getPlayerBalance() == -1f) {
        text.append(Text.literal("?").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
      } else if (Journal.INSTANCE.getMetadata().getPlayerBalance() >= entry.getCost()) {
        text.append(Text.literal("✔").formatted(Formatting.DARK_GREEN));
      } else {
        text.append(Text.literal("✖").formatted(Formatting.DARK_RED));
      }
      text.append(Text.literal(" " + entry.getCost() + " Coin").formatted(Formatting.DARK_GRAY));
      context.drawText(textRenderer, text, x + 20, y + 4, 0x404040, false);
      y += 16;
    }

    // Render Recipe Known
    if (entry.getRecipeKnown() != -1) {
      context.drawItem(new ItemStack(Items.BOOK), x, y);

      // TODO: Get player's known recipes
      MutableText text = Text.literal(entry.getRecipeKnown() == 1 ? "✔" : "✖").formatted(entry.getRecipeKnown() == 1 ? Formatting.DARK_GREEN : Formatting.DARK_RED);
      text.append(
          Text.literal(entry.getRecipeKnown() == 1 ? " Recipe Known" : " Recipe Unknown")
              .formatted(Formatting.DARK_GRAY)
      );
      context.drawText(textRenderer, text, x + 20, y + 4, 0x404040, false);
      y += 16;
    }

    // Render Required Class
    if (entry.getRequiredLevel() != -1) {
      context.drawItem(new ItemStack(Items.TURTLE_EGG), x, y);

      MutableText text = Text.empty();
      if (Journal.INSTANCE == null || Journal.INSTANCE.getMetadata().getProfessionLevels().get(entry.getRequiredClass()) == null) {
        text.append(Text.literal("?").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
      } else if (Journal.INSTANCE.getMetadata().getProfessionLevels().get(entry.getRequiredClass()) >= entry.getRequiredLevel()) {
        text.append(Text.literal("✔").formatted(Formatting.DARK_GREEN));
      } else {
        text.append(Text.literal("✖").formatted(Formatting.DARK_RED));
      }
      text.append(
          Text
              .literal(" Requires " + entry.getRequiredLevel() + " in " + entry.getRequiredClass())
              .formatted(Formatting.DARK_GRAY)
      );
      context.drawText(textRenderer, text, x + 20, y + 4, 0x404040, false);
      y += 16;
    }

    // Render Items
    for (ItemStack item : entry.getIngredientItems()) {
      // Render item
      context.drawItem(item, x, y);

      // Render text
      int requiredCount = this.requiredItems(item);

      MutableText text = Text.literal(requiredCount > 0 ? "✖ " : "✔ ").formatted(requiredCount > 0 ? Formatting.DARK_RED : Formatting.DARK_GREEN);
      text.append(Text.literal(JournalEntryBuilder.getName(item)).formatted(Formatting.DARK_GRAY));
      if (item.getCount() > 1) {
        text.append(Text.literal(" x" + item.getCount()).formatted(Formatting.DARK_GRAY));
      }
      context.drawText(textRenderer, text, x + 20, y + 4, 0x404040, false);

      y += 16;
    }
  }

  private void goToPage(int page) {
    this.page = page;

    this.nextPageButton.visible = this.page < this.entries.size() - 1;
    this.prevPageButton.visible = this.page > 0;
  }

  private int requiredItems(ItemStack stack) {
    if (this.inventory == null) {
      return stack.getCount();
    }

    if (!this.inventory.contains(stack)) {
      return stack.getCount();
    }

    // Check if the inventory contains the required count
    int requiredCount = stack.getCount();
    for (ItemStack item : this.inventory.main) {
      if (item.getItem() == stack.getItem()) {
        requiredCount -= item.getCount();
      }
    }

    return requiredCount;
  }
}
