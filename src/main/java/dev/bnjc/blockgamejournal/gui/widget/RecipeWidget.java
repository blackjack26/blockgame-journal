package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gui.screen.RecipeDisplay;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.JournalEntryBuilder;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeWidget extends ClickableWidget {
  private final Screen parent;
  private final TextRenderer textRenderer;

  @Setter
  private @Nullable JournalEntry entry;
  private PlayerInventory inventory;

  private double scrollY;
  private int scrollTop;
  private int lastY;
  private boolean scrollbarDragged;
  private Map<String, Integer[]> ingredientPositions;

  public RecipeWidget(Screen parent, int x, int y, int width, int height) {
    super(x, y, width, height, Text.empty());

    this.parent = parent;
    this.textRenderer = MinecraftClient.getInstance().textRenderer;
    this.entry = null;

    this.scrollbarDragged = false;
    this.scrollTop = y + 2;
    this.lastY = y + 2;
    setScrollY(0.0);

    // Populate inventory
    Entity entity = MinecraftClient.getInstance().getCameraEntity();
    if (entity instanceof ClientPlayerEntity player) {
      this.inventory = player.getInventory();
    }
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (!this.visible) {
      return false;
    }

    if (this.overflows() &&
        mouseX >= (double) (this.getX() + this.getWidth() - 6) &&
        mouseX <= (double) (this.getX() + this.getWidth()) &&
        mouseY >= (double) this.scrollTop &&
        mouseY <= (double) (this.scrollTop + this.getScrollWindowHeight()) &&
        button == 0
    ) {
      this.scrollbarDragged = true;
      return true;
    }

    boolean withinBounds = this.isWithinBounds(mouseX, mouseY);
    if (withinBounds && button == 0) {
      for (Map.Entry<String, Integer[]> entry : this.ingredientPositions.entrySet()) {
        Integer[] bounds = entry.getValue();
        if (mouseY + scrollY >= bounds[0] && mouseY + scrollY <= bounds[1]) {
          if (Journal.INSTANCE != null && Journal.INSTANCE.hasJournalEntry(entry.getKey())) {
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());
            MinecraftClient.getInstance().setScreen(new RecipeDisplay(entry.getKey(), this.parent));
            return true;
          }
        }
      }
    }

    return false;
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (button == 0) {
      this.scrollbarDragged = false;
    }

    return super.mouseReleased(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    if (!this.visible || !this.isFocused() || !this.scrollbarDragged) {
      return false;
    }

    if (mouseY < (double) this.scrollTop) {
      this.setScrollY(0.0);
    } else if (mouseY > (double) (this.scrollTop + this.getScrollWindowHeight())) {
      this.setScrollY(this.lastY - this.getBottom());
    } else {
      this.setScrollY(this.scrollY + deltaY);
    }

    return true;
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    if (!this.visible) {
      return false;
    }

    this.setScrollY(this.scrollY - verticalAmount * 9.0);
    return true;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    boolean up = keyCode == 265;
    boolean down = keyCode == 264;
    if (up || down) {
      double d = this.scrollY;
      this.setScrollY(this.scrollY + (up ? -1 : 1) * 9.0);
      if (d != this.scrollY) {
        return true;
      }
    }

    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    this.renderRecipeItem(context);
    this.renderNpcName(context);

    this.scrollTop = this.lastY;
    context.enableScissor(this.getX(), this.scrollTop, this.getX() + this.getWidth(), this.getY() + this.getHeight());
    context.getMatrices().push();
    context.getMatrices().translate(0.0, -this.scrollY, 0.0);
    this.renderEntries(context);
    context.getMatrices().pop();
    context.disableScissor();

    if (this.overflows()) {
      this.renderScrollbar(context);
    }
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {

  }

  private void renderRecipeItem(DrawContext context) {
    if (this.entry == null) {
      return;
    }

    // Item Icon
    ItemStack item = this.entry.getItem();
    if (item == null) {
      return;
    }

    int x = this.getX() + this.getWidth() / 2 - 8;
    int y = this.getY() + 2;
    context.drawItem(item, x, y);

    // Title
    MutableText title = Text.literal(JournalEntryBuilder.getName(item)).formatted(Formatting.BOLD, Formatting.WHITE);
    List<OrderedText> lines = this.textRenderer.wrapLines(title, this.getWidth() - 20);
    lastY = this.getY() + 20;


    for (OrderedText oText : lines) {
      int titleX = this.getX() + this.getWidth() / 2 - this.textRenderer.getWidth(oText) / 2;
      context.drawText(textRenderer, oText, titleX, lastY, 0xFFFFFF, true);
      lastY += 10;
    }
  }

  private void renderNpcName(DrawContext context) {
    if (this.entry == null) {
      return;
    }

    // Render NPC Name
    MutableText npcText = Text.literal("Crafted by ").formatted(Formatting.DARK_AQUA, Formatting.ITALIC); // TODO: Translation
    npcText.append(Text.literal(entry.getNpcName()).formatted(Formatting.DARK_AQUA, Formatting.BOLD));

    // Center Title
    int titleX = this.getX() + this.getWidth() / 2 - this.textRenderer.getWidth(npcText) / 2;
    context.drawText(this.textRenderer, npcText, titleX, this.lastY + 2, 0x404040, false);

    this.lastY += 20;
  }

  private void renderEntries(DrawContext context) {
    if (entry == null) {
      // TODO: Render empty entry
      return;
    }

    this.renderCost(context);
    this.renderRecipeKnown(context);
    this.renderRequiredClass(context);
    this.renderIngredients(context);
  }

  private void renderCost(DrawContext context) {
    if (this.entry == null || this.entry.getCost() <= 0) {
      return;
    }

    int x = this.getX();
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

  private void renderRecipeKnown(DrawContext context) {
    if (this.entry == null || this.entry.getRecipeKnown() == -1) {
      return;
    }

    int x = this.getX();
    context.drawItem(new ItemStack(Items.BOOK), x, this.lastY);

    // TODO: Get player's known recipes
    MutableText text = Text.literal(entry.getRecipeKnown() == 1 ? "✔" : "✖").formatted(entry.getRecipeKnown() == 1 ? Formatting.DARK_GREEN : Formatting.DARK_RED);
    text.append(Text.literal(" Recipe Known").formatted(Formatting.DARK_GRAY));
    context.drawText(textRenderer, text, x + 20, this.lastY + 4, 0x404040, false);

    this.lastY += 16;
  }

  private void renderRequiredClass(DrawContext context) {
    if (this.entry == null || this.entry.getRequiredLevel() == -1) {
      return;
    }

    int x = this.getX();

    context.drawItem(new ItemStack(Items.TURTLE_EGG), x, this.lastY);

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
    context.drawText(textRenderer, text, x + 20, this.lastY + 4, 0x404040, false);

    this.lastY += 16;
  }

  private void renderIngredients(DrawContext context) {
    this.ingredientPositions = new HashMap<>();

    if (this.entry == null) {
      return;
    }

    int x = this.getX();

    for (ItemStack item : entry.getIngredientItems()) {
      String itemKey = JournalEntryBuilder.getKey(item);
      int startY = this.lastY;

      // Render item
      context.drawItem(item, x, this.lastY);

      // Render text
      int requiredCount = this.requiredItems(item);

      MutableText text = Text.literal(requiredCount > 0 ? "✖ " : "✔ ").formatted(requiredCount > 0 ? Formatting.DARK_RED : Formatting.DARK_GREEN);
      text.append(Text.literal(JournalEntryBuilder.getName(item)).formatted(Formatting.DARK_GRAY));
      if (item.getCount() > 1) {
        text.append(Text.literal(" x" + item.getCount()).formatted(Formatting.DARK_GRAY));
      }

      List<OrderedText> lines = this.textRenderer.wrapLines(text, this.getWidth() - 20);
      for (OrderedText oText : lines) {
        context.drawText(textRenderer, oText, x + 20, this.lastY + 4, 0x404040, false);
        this.lastY += 10;
      }

      int endY = this.lastY;
      this.ingredientPositions.put(itemKey, new Integer[]{startY, endY});

      this.lastY += 6;
    }
  }

  private void renderScrollbar(DrawContext context) {
    int height = this.getScrollbarThumbHeight();
    int x = this.getX() + this.getWidth();
    int y = Math.max(this.scrollTop, this.scrollTop + (int) this.scrollY / (this.lastY - this.getBottom()) * (this.getScrollWindowHeight() - height));
    context.drawGuiTexture(GuiUtil.sprite("scroller"), x - 6, y, 6, height);
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

  private void setScrollY(double scrollY) {
    if (!this.overflows()) {
      this.scrollY = 0.0;
      return;
    }

    this.scrollY = MathHelper.clamp(scrollY, 0.0, this.lastY - this.getBottom());
  }

  private boolean overflows() {
    return this.lastY > this.getBottom();
  }

  private int getBottom() {
    return this.getY() + this.getHeight();
  }

  private int getContentsHeight() {
    return this.lastY - this.scrollTop;
  }

  private int getScrollWindowHeight() {
    return this.getBottom() - this.scrollTop;
  }

  private int getScrollbarThumbHeight() {
    int height = this.getScrollWindowHeight();
    return MathHelper.clamp((int)((float)(height * height) / (float) this.getContentsHeight()), 32, height);
  }

  private boolean isWithinBounds(double mouseX, double mouseY) {
    return mouseX >= (double)this.getX() && mouseX < (double)(this.getX() + this.width) && mouseY >= (double)this.getY() && mouseY < (double)(this.getY() + this.height);
  }
}
