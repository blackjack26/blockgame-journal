package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gui.screen.RecipeScreen;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.recipe.JournalPlayerInventory;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import dev.bnjc.blockgamejournal.util.Profession;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeWidget extends ClickableWidget {
  private final Screen parent;
  private final TextRenderer textRenderer;

  @Setter
  private @Nullable JournalEntry entry;
  private final JournalPlayerInventory inventory;

  private double scrollY;
  private int scrollTop;
  private int lastY;
  private boolean scrollbarDragged;
  private Map<String, Integer[]> ingredientPositions;

  private List<Text> tooltip;

  public RecipeWidget(Screen parent, int x, int y, int width, int height) {
    super(x, y, width, height, Text.empty());

    this.parent = parent;
    this.textRenderer = MinecraftClient.getInstance().textRenderer;
    this.entry = null;

    this.scrollbarDragged = false;
    this.scrollTop = y + 2;
    this.lastY = y + 2;
    setScrollY(0.0);

    this.inventory = JournalPlayerInventory.defaultInventory();
    this.tooltip = new ArrayList<>();
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
            MinecraftClient.getInstance().setScreen(new RecipeScreen(entry.getKey(), this.parent));
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
      this.setScrollY(this.getMaxScrollY());
    } else {
      int h = this.getScrollbarThumbHeight();
      double d = Math.max(1, this.getMaxScrollY() / (this.getScrollWindowHeight() - h));
      this.setScrollY(this.scrollY + deltaY * d);
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
    if (!this.tooltip.isEmpty()) {
      context.drawTooltip(textRenderer, this.tooltip, mouseX, mouseY);
    }
    this.tooltip.clear();

    this.renderRecipeItem(context, mouseX, mouseY);
    this.renderNpcName(context);

    this.scrollTop = this.lastY;
    context.enableScissor(this.getX(), this.scrollTop, this.getX() + this.getWidth(), this.getY() + this.getHeight());
    context.getMatrices().push();
    context.getMatrices().translate(0.0, -this.scrollY, 0.0);
    this.renderEntries(context, mouseX, mouseY);
    context.getMatrices().pop();
    context.disableScissor();

    if (this.overflows()) {
      this.renderScrollbar(context);
    }
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {

  }

  private void renderRecipeItem(DrawContext context, int mouseX, int mouseY) {
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

    boolean renderUnavailable = this.entry.isUnavailable();
    if (renderUnavailable) {
      context.getMatrices().push();
      context.getMatrices().translate(0, 0, 150);
      context.drawGuiTexture(GuiUtil.sprite("warning_icon"), x, y - 2, 150, 8, 8);
      context.getMatrices().pop();
    }

    boolean recipeKnown = this.entry.isRecipeKnown();
    boolean profReqMet = this.entry.meetsProfessionRequirements();
    boolean renderLock = !(recipeKnown && profReqMet) && BlockgameJournal.getConfig().getGeneralConfig().showRecipeLock;
    if (renderLock) {
      context.getMatrices().push();
      context.getMatrices().translate(0, 0, 150);
      context.drawGuiTexture(GuiUtil.sprite("lock_icon"), x + 10, y - 2, 150, 8, 8);
      context.getMatrices().pop();
    }

    if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
      List<Text> tooltip = new ArrayList<>();
      if (renderUnavailable) {
        tooltip.add(Text.literal("⚠ Not available from " + entry.getNpcName() + " ⚠").formatted(Formatting.RED));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("This recipe has either moved vendors").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("or is no longer available for crafting.").formatted(Formatting.GRAY));
      }

      if (renderLock) {
        if (renderUnavailable) {
          // Add a spacer
          tooltip.add(Text.empty());
        }

        tooltip.add(Text.literal("Recipe locked").formatted(Formatting.RED));
        if (!recipeKnown) {
          tooltip.add(Text.literal("- Recipe not known").formatted(Formatting.GRAY));
        }
        if (!profReqMet) {
          tooltip.add(Text.literal("- Profession level too low").formatted(Formatting.GRAY));
        }
      }

      if (!tooltip.isEmpty()) {
        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
      }
    }

    // Item count (in bottom right corner of item)
    ItemUtil.renderItemCount(context, x, y, this.entry.getCount());

    // Title
    MutableText title = Text.literal(ItemUtil.getName(item)).formatted(Formatting.BOLD, Formatting.WHITE);
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

    this.lastY += 16;
  }

  private void renderEntries(DrawContext context, int mouseX, int mouseY) {
    if (entry == null) {
      // TODO: Render empty entry
      return;
    }

    this.renderCost(context, mouseX, mouseY);
    this.renderRecipeKnown(context);
    this.renderRequiredClass(context, mouseX, mouseY);
    this.renderIngredients(context);
  }

  private void renderCost(DrawContext context, int mouseX, int mouseY) {
    if (this.entry == null || this.entry.getCost() <= 0) {
      return;
    }

    int x = this.getX();
    context.drawItem(ItemUtil.getGoldItem((int) this.entry.getCost()), x, this.lastY);

    int iconX = x + 20;
    int iconY = this.lastY + 4;
    MutableText text = Text.empty();
    if (Journal.INSTANCE == null || Journal.INSTANCE.getMetadata().getPlayerBalance() == -1f) {
      text.append(Text.literal("?").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));

      if (mouseX >= x && mouseX < x + this.getWidth() && mouseY >= this.lastY && mouseY < this.lastY + 16) {
        tooltip.add(Text.literal("Player balance unknown").formatted(Formatting.WHITE));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Run the ").formatted(Formatting.GRAY)
            .append(Text.literal("/balance").formatted(Formatting.YELLOW))
            .append(Text.literal(" command").formatted(Formatting.GRAY))
        );
        tooltip.add(Text.literal("to update your balance").formatted(Formatting.GRAY));
      }

    } else if (Journal.INSTANCE.getMetadata().getPlayerBalance() >= entry.getCost()) {
      text.append(Text.literal("✔").formatted(Formatting.DARK_GREEN));
    } else {
      text.append(Text.literal("✖").formatted(Formatting.DARK_RED));
    }
    context.drawText(textRenderer, text, iconX, iconY, 0x404040, false);

    MutableText coinText = Text.literal(entry.getCost() + " Coin").formatted(Formatting.DARK_GRAY);
    context.drawText(textRenderer, coinText, iconX + 12, iconY, 0x404040, false);

    this.lastY += 16;
  }

  private void renderRecipeKnown(DrawContext context) {
    if (this.entry == null) {
      return;
    }

    Boolean recipeKnown = Journal.INSTANCE.getMetadata().getKnownRecipe(entry.getKey());
    if (recipeKnown == null) {
      return;
    }

    int x = this.getX();
    context.drawItem(new ItemStack(Items.BOOK), x, this.lastY);

    MutableText text = Text.literal(recipeKnown ? "✔" : "✖").formatted(recipeKnown ? Formatting.DARK_GREEN : Formatting.DARK_RED);
    text.append(Text.literal(" Recipe Known").formatted(Formatting.DARK_GRAY));
    context.drawText(textRenderer, text, x + 20, this.lastY + 4, 0x404040, false);

    this.lastY += 16;
  }

  private void renderRequiredClass(DrawContext context, int mouseX, int mouseY) {
    if (this.entry == null || this.entry.getRequiredLevel() == -1 || Journal.INSTANCE == null) {
      return;
    }

    int x = this.getX();
    context.drawItem(Profession.getIcon(entry.getRequiredClass()), x, this.lastY);

    @Nullable Integer profLevel = Journal.INSTANCE.getMetadata().getProfessionLevels().get(entry.getRequiredClass());
    MutableText text = Text.empty();
    if (profLevel == null) {
      text.append(Text.literal("?").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));

      if (mouseX >= x && mouseX < x + this.getWidth() && mouseY >= this.lastY && mouseY < this.lastY + 16) {
        tooltip.add(Text.literal("Profession level unknown").formatted(Formatting.WHITE));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Run the ").formatted(Formatting.GRAY)
            .append(Text.literal("/profile").formatted(Formatting.YELLOW))
            .append(Text.literal(" command").formatted(Formatting.GRAY))
        );
        tooltip.add(Text.literal("to update your professions").formatted(Formatting.GRAY));
      }

    } else if (profLevel >= entry.getRequiredLevel()) {
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
      String itemKey = ItemUtil.getKey(item);
      int startY = this.lastY;
      int neededCount = this.inventory.neededCount(item);
      boolean hasEnough = neededCount <= 0;

      // Render item
      context.drawItem(item, x, this.lastY);

      // Render text
      MutableText text = Text.literal(hasEnough ? "✔ " : "✖ ").formatted(hasEnough ? Formatting.DARK_GREEN : Formatting.DARK_RED);
      MutableText itemText = Text.literal(ItemUtil.getName(item)).formatted(Formatting.DARK_GRAY);

      if (Journal.INSTANCE != null && Journal.INSTANCE.hasJournalEntry(itemKey)) {
        itemText.formatted(Formatting.UNDERLINE);
      }
      text.append(itemText);

      if (item.getCount() > 1) {
        MutableText countText = Text.literal(" x" + item.getCount());
        countText.setStyle(countText.getStyle().withColor(0x8A8A8A));
        text.append(countText);
      }

      List<OrderedText> lines = this.textRenderer.wrapLines(text, this.getWidth() - 20);
      for (OrderedText oText : lines) {
        context.drawText(textRenderer, oText, x + 20, this.lastY + 4, 0x404040, false);
        this.lastY += 10;
      }
      this.lastY += 6;

      int endY = this.lastY;
      this.ingredientPositions.put(itemKey, new Integer[]{startY, endY});
    }
  }

  private void renderScrollbar(DrawContext context) {
    int height = this.getScrollbarThumbHeight();
    int x = this.getX() + this.getWidth();
    int y = Math.max(this.scrollTop, (int) this.scrollY * (this.getScrollWindowHeight() - height) / this.getMaxScrollY() + this.scrollTop);

    context.drawGuiTexture(GuiUtil.sprite("scroller"), x - 6, y, 6, height);
  }

  private void setScrollY(double scrollY) {
    if (!this.overflows()) {
      this.scrollY = 0.0;
      return;
    }

    this.scrollY = MathHelper.clamp(scrollY, 0.0, this.getMaxScrollY());
  }

  private int getMaxScrollY() {
    return this.lastY - this.getBottom();
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
