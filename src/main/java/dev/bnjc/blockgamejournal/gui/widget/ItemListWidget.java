package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.gui.screen.JournalScreen;
import dev.bnjc.blockgamejournal.gui.screen.RecipeDisplay;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalMode;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.Collections;
import java.util.List;

import static dev.bnjc.blockgamejournal.gui.screen.JournalScreen.GRID_SLOT_SIZE;

public class ItemListWidget extends ClickableWidget {
  private static final Identifier BACKGROUND = GuiUtil.sprite("widgets/slot_background");

  private final Screen parent;
  private final int gridWidth;
  private final int gridHeight;
  private List<ItemStack> items = Collections.emptyList();
  private int offset = 0;

  @Setter
  private JournalMode.Type mode = JournalMode.Type.ITEM_SEARCH;

  @Setter
  private boolean hideTooltip;

  public ItemListWidget(Screen parent, int x, int y, int gridWidth, int gridHeight) {
    super(x, y, gridWidth * GRID_SLOT_SIZE, gridHeight * GRID_SLOT_SIZE, Text.empty());

    this.parent = parent;
    this.gridWidth = gridWidth;
    this.gridHeight = gridHeight;
  }

  public void setItems(List<ItemStack> items) {
    this.items = items;
    int rows = getRows();
    this.offset = MathHelper.clamp(this.offset, 0, Math.max((rows - gridHeight) * gridWidth, 0));
  }

  public int getRows() {
    return (int) Math.ceil((double) this.items.size() / this.gridWidth);
  }

  public void onScroll(float progress) {
    int rows = getRows();
    if (rows <= this.gridHeight) {
      return; // don't scroll if there's nothing to scroll
    }

    int range = rows - this.gridHeight;
    int rowOffset = (int) (progress * (range + 0.5f));
    this.offset = rowOffset * this.gridWidth;
  }

  @Override
  public void onClick(double mouseX, double mouseY) {
    List<ItemStack> items = this.getOffsetItems();
    int x = (int) ((mouseX - this.getX()) / GRID_SLOT_SIZE);
    int y = (int) ((mouseY - this.getY()) / GRID_SLOT_SIZE);
    int index = (y * this.gridWidth) + x;
    if (index >= items.size()) {
      return;
    }

    ItemStack item = items.get(index);

    if (this.mode == JournalMode.Type.ITEM_SEARCH) {
      // Open RecipeDisplay screen
      MinecraftClient.getInstance().setScreen(new RecipeDisplay(item, this.parent));
    }
    else if (this.mode == JournalMode.Type.NPC_SEARCH) {
      if (item.getItem() instanceof PlayerHeadItem) {
        if (this.parent instanceof JournalScreen journalScreen && item.hasNbt()) {
          journalScreen.setSelectedNpc(item.getNbt().getString(Journal.NPC_NAME_KEY));
        }
      } else {
        MinecraftClient.getInstance().setScreen(new RecipeDisplay(item, this.parent));
      }
    }
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    context.drawGuiTexture(BACKGROUND, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    this.renderItems(context);
    this.renderTooltip(context, mouseX, mouseY);
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {

  }

  private void renderItems(DrawContext context) {
    List<ItemStack> items = this.getOffsetItems();

    for (int i = 0; i < (this.gridWidth * this.gridHeight); i++) {
      int x = this.getX() + GRID_SLOT_SIZE * (i % this.gridWidth);
      int y = this.getY() + GRID_SLOT_SIZE * (i / this.gridWidth);
      if (i < items.size()) {
        ItemStack item = items.get(i);
        context.drawItem(item, x + 1, y + 1);
      }
    }
  }

  private void renderTooltip(DrawContext context, int mouseX, int mouseY) {
    List<ItemStack> items = this.getOffsetItems();
    if (!this.isHovered()) {
      return;
    }

    int x = (mouseX - this.getX()) / GRID_SLOT_SIZE;
    int y = (mouseY - this.getY()) / GRID_SLOT_SIZE;
    if (x < 0 || x > this.gridWidth || y < 0 || y > this.gridHeight) {
      return;
    }

    int index = (y * this.gridWidth) + x;
    if (index >= items.size()) {
      return;
    }

    int slotX = this.getX() + x * GRID_SLOT_SIZE;
    int slotY = this.getY() + y * GRID_SLOT_SIZE;
    context.fill(slotX + 1, slotY + 1, slotX + GRID_SLOT_SIZE - 1, slotY + GRID_SLOT_SIZE - 1, 0x80_FFFFFF);

    if (!this.hideTooltip) {
      ItemStack stack = items.get(index);

      context.getMatrices().push();
      context.getMatrices().translate(0, 0, 150f);
      context.drawItemTooltip(MinecraftClient.getInstance().textRenderer, stack, mouseX, mouseY);
      context.getMatrices().pop();
    }
  }

  private List<ItemStack> getOffsetItems() {
    if (this.items.isEmpty()) {
      return Collections.emptyList();
    }

    int min = MathHelper.clamp(this.offset, 0, this.items.size() - 1);
    int max = MathHelper.clamp(this.offset + gridWidth * gridHeight, 0, this.items.size());
    return this.items.subList(min, max);
  }
}
