package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gui.screen.JournalScreen;
import dev.bnjc.blockgamejournal.gui.screen.RecipeScreen;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.JournalItemStack;
import dev.bnjc.blockgamejournal.journal.JournalMode;
import dev.bnjc.blockgamejournal.journal.npc.NPCEntity;
import dev.bnjc.blockgamejournal.journal.npc.NPCEntry;
import dev.bnjc.blockgamejournal.journal.npc.NPCItemStack;
import dev.bnjc.blockgamejournal.journal.npc.NPCUtil;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static dev.bnjc.blockgamejournal.gui.screen.JournalScreen.GRID_SLOT_SIZE;

public class ItemListWidget extends ClickableWidget {
  private static final Identifier BACKGROUND = GuiUtil.sprite("widgets/slot_background");

  private final Screen parent;
  private final int gridWidth;
  private final int gridHeight;

  private List<JournalItemStack> items = Collections.emptyList();
  private int offset = 0;

  @Setter
  private VerticalScrollWidget scroll;

  private @Nullable ItemStack hoveredItem = null;

  @Setter
  private JournalMode.Type mode = JournalMode.Type.ITEM_SEARCH;

  @Setter
  private boolean hideTooltip;
  private int lastButton = -1;

  public ItemListWidget(Screen parent, int x, int y, int gridWidth, int gridHeight) {
    super(x, y, gridWidth * GRID_SLOT_SIZE, gridHeight * GRID_SLOT_SIZE, Text.empty());

    this.parent = parent;
    this.gridWidth = gridWidth;
    this.gridHeight = gridHeight;
  }

  public void setItems(List<JournalItemStack> items) {
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
  public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    if (this.scroll == null) {
      return false;
    }

    return this.scroll.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
  }

  @Override
  protected boolean isValidClickButton(int button) {
    this.lastButton = button;
    return button == 0 || button == 1;
  }

  @Override
  public void onClick(double mouseX, double mouseY) {
    List<JournalItemStack> items = this.getOffsetItems();
    int x = (int) ((mouseX - this.getX()) / GRID_SLOT_SIZE);
    int y = (int) ((mouseY - this.getY()) / GRID_SLOT_SIZE);
    int index = (y * this.gridWidth) + x;

    ItemStack item;

    if (this.useSlotPositions()) {
      item = null;
      for (JournalItemStack i : items) {
        if (i.getSlot() == index) {
          item = i.getStack();
          break;
        }
      }
    } else {
      if (index >= items.size()) {
        return;
      }
      item = items.get(index).getStack();
    }

    BlockgameJournal.LOGGER.debug("Clicked item: {}, Slot: {}", item, index);
    if (item == null || Journal.INSTANCE == null) {
      return;
    }

    if (this.lastButton == 1) {
      if (this.mode == JournalMode.Type.FAVORITES ||
          this.mode == JournalMode.Type.ITEM_SEARCH ||
          (this.mode == JournalMode.Type.NPC_SEARCH && JournalScreen.getSelectedNpc() != null) ||
          (this.mode == JournalMode.Type.INGREDIENT_SEARCH && JournalScreen.getSelectedIngredient() != null)) {

        @Nullable JournalEntry entry = null;

        if (this.mode == JournalMode.Type.ITEM_SEARCH) {
          // Just get the first entry
          entry = Journal.INSTANCE.getFirstJournalEntry(ItemUtil.getKey(item));
        }
        else if (this.mode == JournalMode.Type.FAVORITES) {
          // Get the first entry that is favorited
          entry = Journal.INSTANCE.getEntries().getOrDefault(ItemUtil.getKey(item), new ArrayList<>())
              .stream()
              .filter(JournalEntry::isFavorite)
              .findFirst()
              .orElse(null);
        }
        else if (this.mode == JournalMode.Type.NPC_SEARCH) {
          // Get the first entry that matches the selected NPC
          entry = Journal.INSTANCE.getEntries().getOrDefault(ItemUtil.getKey(item), new ArrayList<>())
              .stream()
              .filter(e -> {
                if (JournalScreen.getSelectedNpc() == null) {
                  return true;
                }

                boolean npcMatch = e.getNpcName().equals(JournalScreen.getSelectedNpc().getNpcWorldName());
                if (useSlotPositions()) {
                  return e.getSlot() == index && npcMatch;
                }
                return npcMatch;
              })
              .findFirst()
              .orElse(null);
        }
        else if (this.mode == JournalMode.Type.INGREDIENT_SEARCH) {
          // Get the first entry that matches the selected ingredient
          entry = Journal.INSTANCE.getEntries().getOrDefault(ItemUtil.getKey(item), new ArrayList<>())
              .stream()
              .filter(e -> {
                if (JournalScreen.getSelectedIngredient() == null) {
                  return true;
                }

                return e.getIngredients().containsKey(ItemUtil.getKey(JournalScreen.getSelectedIngredient()));
              })
              .findFirst()
              .orElse(null);
        }

        if (entry != null) {
          entry.setTracked(!entry.isTracked());
          JournalScreen js = getJournalScreen();
          if (js != null) js.refreshTracking();
          return;
        }
      }
    }

    if (this.mode == JournalMode.Type.ITEM_SEARCH) {
      // Open RecipeDisplay screen
      MinecraftClient.getInstance().setScreen(new RecipeScreen(item, this.parent));
    }
    else if (this.mode == JournalMode.Type.FAVORITES) {
      RecipeScreen recipeScreen = new RecipeScreen(item, this.parent);
      recipeScreen.filterEntries(JournalEntry::isFavorite);

      MinecraftClient.getInstance().setScreen(recipeScreen);
    }
    else if (this.mode == JournalMode.Type.NPC_SEARCH) {
      if (item.getItem() instanceof PlayerHeadItem || item.getItem() instanceof SpawnEggItem) {
        if (this.parent instanceof JournalScreen journalScreen && item.hasNbt()) {
          journalScreen.setSelectedNpc(item.getNbt().getString(Journal.NPC_NAME_KEY));
        }
      } else {
        RecipeScreen recipeScreen = new RecipeScreen(item, this.parent);
        NPCEntity selectedNpc = JournalScreen.getSelectedNpc();
        recipeScreen.filterEntries(entry -> {
          if (selectedNpc == null) {
            // If no NPC is selected, show all recipes
            return true;
          }

          // Only show recipes that the selected NPC can craft
          boolean isNpc = entry.getNpcName().equals(selectedNpc.getNpcWorldName());

          if (useSlotPositions()) {
            return entry.getSlot() == index && isNpc;
          }

          return isNpc;
        });

        MinecraftClient.getInstance().setScreen(recipeScreen);
      }
    }
    else if (this.mode == JournalMode.Type.INGREDIENT_SEARCH) {
      if (this.parent instanceof JournalScreen journalScreen && JournalScreen.getSelectedIngredient() == null) {
        journalScreen.setSelectedIngredient(item);
      } else {
        RecipeScreen recipeScreen = new RecipeScreen(item, this.parent);
        recipeScreen.filterEntries(entry -> {
          if (JournalScreen.getSelectedIngredient() == null) {
            // If no ingredient is selected, show all recipes
            return true;
          }

          // Only show recipes that use the selected ingredient
          return entry.getIngredients().containsKey(ItemUtil.getKey(JournalScreen.getSelectedIngredient()));
        });

        MinecraftClient.getInstance().setScreen(recipeScreen);
      }
    }
  }

  @Override
  public boolean charTyped(char chr, int modifiers) {
    boolean handled = this.handledChar(chr);
    return handled || super.charTyped(chr, modifiers);
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!this.visible) {
      return;
    }

    if (!this.items.isEmpty()) {
      context.drawGuiTexture(BACKGROUND, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }
    this.renderItems(context);
    this.renderTooltip(context, mouseX, mouseY);
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {

  }

  private boolean handledChar(char keyCode) {
    if (this.hoveredItem == null || Journal.INSTANCE == null) {
      return false;
    }

    if (this.mode == JournalMode.Type.NPC_SEARCH &&
        (this.hoveredItem.getItem() instanceof PlayerHeadItem || this.hoveredItem.getItem() instanceof SpawnEggItem)) {
      String npcName = this.hoveredItem.getNbt().getString(Journal.NPC_NAME_KEY);
      var npcEntry = Journal.INSTANCE.getKnownNpc(npcName);

      if (npcEntry.isPresent()) {
        NPCEntry npc = npcEntry.get();

        if (keyCode == 'a' || keyCode == 'A') {
          // If NPC is locating and the A key is pressed, stop locating
          if (npc.isLocating()) {
            npc.setLocating(false);
            NPCItemStack.updateStack(npcName, this.hoveredItem, npc);
            return true;
          }

          // If NPC is not locating and the A key is pressed, start locating
          if (npc.getPosition() != null) {
            npc.setLocating(true);
            NPCItemStack.updateStack(npcName, this.hoveredItem, npc);
            return true;
          }
        }
        else if (keyCode == 'x' || keyCode == 'X') {
          // Remove NPC from known NPCs
          NPCUtil.removeNPC(npcName);
          MinecraftClient.getInstance()
              .getToastManager()
              .add(new SystemToast(
                  SystemToast.Type.PERIODIC_NOTIFICATION,
                  Text.of("Vendor Removed!"),
                  Text.of(npcName)
              ));

          if (this.parent instanceof JournalScreen journalScreen) {
            journalScreen.refreshItems();
          }

          return true;
        }
      }
    }
    else if (this.mode == JournalMode.Type.FAVORITES) {
      if (keyCode == 'a' || keyCode == 'A') {
        if (Journal.INSTANCE.hasJournalEntry(this.hoveredItem)) {
          List<JournalEntry> entries = Journal.INSTANCE.getEntries().getOrDefault(ItemUtil.getKey(this.hoveredItem), new ArrayList<>());
          for (JournalEntry entry : entries) {
            entry.setFavorite(false);
          }

          if (this.parent instanceof JournalScreen journalScreen) {
            journalScreen.refreshItems();
          }
          return true;
        }
      }
    }
    else if (this.mode == JournalMode.Type.ITEM_SEARCH) {
      if (keyCode == 'x' || keyCode == 'X') {
        if (Journal.INSTANCE.hasJournalEntry(this.hoveredItem)) {
          Journal.INSTANCE.removeAllEntries(ItemUtil.getKey(this.hoveredItem));

          MinecraftClient.getInstance()
              .getToastManager()
              .add(new SystemToast(
                  SystemToast.Type.PERIODIC_NOTIFICATION,
                  Text.of("Recipe(s) Removed!"),
                  Text.of(ItemUtil.getName(this.hoveredItem))
              ));

          if (this.parent instanceof JournalScreen journalScreen) {
            journalScreen.refreshItems();
          }
          return true;
        }
      }
    }

    return false;
  }

  private void renderItems(DrawContext context) {
    List<JournalItemStack> items = this.getOffsetItems();

    if (items.isEmpty()) {
      this.renderNoItemsMessage(context);
      return;
    }

    if (this.useSlotPositions()) {
      for (JournalItemStack item : items) {
        int x = this.getX() + GRID_SLOT_SIZE * (item.getSlot() % this.gridWidth);
        int y = this.getY() + GRID_SLOT_SIZE * (item.getSlot() / this.gridWidth);
        this.renderItem(context, item.getStack(), x, y, item.getSlot());
      }
    } else {
      for (int i = 0; i < (this.gridWidth * this.gridHeight); i++) {
        int x = this.getX() + GRID_SLOT_SIZE * (i % this.gridWidth);
        int y = this.getY() + GRID_SLOT_SIZE * (i / this.gridWidth);
        if (i < items.size()) {
          JournalItemStack item = items.get(i);
          this.renderItem(context, item.getStack(), x, y, item.getSlot());
        }
      }
    }
  }

  private void renderNoItemsMessage(DrawContext context) {
    Text message = Text.literal("No recipes found.").formatted(Formatting.WHITE, Formatting.BOLD);
    int width = MinecraftClient.getInstance().textRenderer.getWidth(message);
    int x = this.getX() + (this.getWidth() - width) / 2;
    int y = this.getY() + (this.getHeight() - MinecraftClient.getInstance().textRenderer.fontHeight) / 2 - 18;
    context.drawText(MinecraftClient.getInstance().textRenderer, message, x, y, 0x000000, true);

    message = Text.literal("Preview vendor items to").formatted(Formatting.DARK_GRAY);
    width = MinecraftClient.getInstance().textRenderer.getWidth(message);
    x = this.getX() + (this.getWidth() - width) / 2;
    y += 12;
    context.drawText(MinecraftClient.getInstance().textRenderer, message, x, y, 0x000000, false);

    message = Text.literal("add them to the journal.").formatted(Formatting.DARK_GRAY);
    width = MinecraftClient.getInstance().textRenderer.getWidth(message);
    x = this.getX() + (this.getWidth() - width) / 2;
    y += 12;
    context.drawText(MinecraftClient.getInstance().textRenderer, message, x, y, 0x000000, false);
  }

  private void renderItem(DrawContext context, ItemStack item, int x, int y, int slot) {
    if (Journal.INSTANCE == null) {
      return;
    }

    if (this.mode == JournalMode.Type.NPC_SEARCH) {
      if (item.getItem() instanceof PlayerHeadItem || item.getItem() instanceof SpawnEggItem) {
        String npcName = item.getNbt().getString(Journal.NPC_NAME_KEY);
        Journal.INSTANCE.getKnownNpc(npcName).ifPresent(npc -> {
          if (npc.isLocating()) {
            context.fill(x + 1, y + 1, x + GRID_SLOT_SIZE - 1, y + GRID_SLOT_SIZE - 1, 0x80_AA00AA);
          }
        });
      }
    }

    if (Journal.INSTANCE.hasJournalEntry(item)) {
      List<JournalEntry> entries = Journal.INSTANCE.getEntries()
          .getOrDefault(ItemUtil.getKey(item), new ArrayList<>())
          .stream().filter((entry) -> {
            if (JournalScreen.getSelectedNpc() == null) {
              return true;
            }

            return entry.getNpcName().toLowerCase(Locale.ROOT).equals(JournalScreen.getSelectedNpc().getNpcWorldName().toLowerCase(Locale.ROOT));
          })
          .toList();

      // Check for locked recipes
      boolean recipeKnown = false;
      boolean profReqMet = false;
      boolean unavailable = true;
      boolean tracked = false;
      for (JournalEntry entry : entries) {
        if (entry.isTracked() && !(useSlotPositions() && entry.getSlot() != slot)) {
          tracked = true;
        }

        Boolean erk = entry.recipeKnown();
        if (erk == null || Boolean.TRUE.equals(erk)) {
          recipeKnown = true;
        }

        if (entry.getRequiredLevel() != -1) {
          int currLevel = Journal.INSTANCE.getMetadata().getProfessionLevels().getOrDefault(entry.getRequiredClass(), -1);
          if (currLevel >= entry.getRequiredLevel()) {
            profReqMet = true;
          }
        } else {
          profReqMet = true;
        }

        if (!entry.isUnavailable()) {
          unavailable = false;
        }
      }

      if (tracked) {
        context.fill(x + 1, y + 1, x + GRID_SLOT_SIZE - 1, y + GRID_SLOT_SIZE - 1, 0x80_00AA00);
      }

      if (!(recipeKnown && profReqMet) && BlockgameJournal.getConfig().getGeneralConfig().showRecipeLock) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 150);
        context.drawGuiTexture(GuiUtil.sprite("lock_icon"), x + 10, y - 2, 150, 8, 8);
        context.getMatrices().pop();
      }

      if (unavailable) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 150);
        context.drawGuiTexture(GuiUtil.sprite("warning_icon"), x, y - 2, 150, 8, 8);
        context.getMatrices().pop();
      }

      int count = -1;

      if (this.useSlotPositions()) {
        count = item.getCount();
      }
      else if (entries.size() == 1) {
        count = entries.get(0).getCount();
      }

      ItemUtil.renderItemCount(context, x, y, count);
    }

    context.drawItem(item, x + 1, y + 1);
  }

  private void renderTooltip(DrawContext context, int mouseX, int mouseY) {
    List<JournalItemStack> items = this.getOffsetItems();
    if (!this.isHovered()) {
      this.hoveredItem = null;
      return;
    }

    int x = (mouseX - this.getX()) / GRID_SLOT_SIZE;
    int y = (mouseY - this.getY()) / GRID_SLOT_SIZE;
    if (x < 0 || x > this.gridWidth || y < 0 || y > this.gridHeight) {
      this.hoveredItem = null;
      return;
    }

    int slotX = this.getX() + x * GRID_SLOT_SIZE;
    int slotY = this.getY() + y * GRID_SLOT_SIZE;
    if (!this.items.isEmpty()) {
      context.fill(slotX + 1, slotY + 1, slotX + GRID_SLOT_SIZE - 1, slotY + GRID_SLOT_SIZE - 1, 0x80_FFFFFF);
    }

    int index = (y * this.gridWidth) + x;

    if (this.useSlotPositions()) {
      this.hoveredItem = null;
      for (JournalItemStack item : items) {
        int slot = item.getSlot();
        if (slot == index) {
          this.hoveredItem = item.getStack();
          break;
        }
      }
    } else {
      if (index >= items.size()) {
        this.hoveredItem = null;
        return;
      }
      this.hoveredItem = items.get(index).getStack();
    }

    if (!this.hideTooltip && this.hoveredItem != null) {
      context.getMatrices().push();
      context.getMatrices().translate(0, 0, 200.0f);
      context.drawItemTooltip(MinecraftClient.getInstance().textRenderer, this.hoveredItem, mouseX, mouseY);
      context.getMatrices().pop();
    }
  }

  private List<JournalItemStack> getOffsetItems() {
    if (this.items.isEmpty()) {
      return Collections.emptyList();
    }

    int min = MathHelper.clamp(this.offset, 0, this.items.size() - 1);
    int max = MathHelper.clamp(this.offset + gridWidth * gridHeight, 0, this.items.size());
    return this.items.subList(min, max);
  }

  private boolean useSlotPositions() {
    return JournalScreen.getVendorItemSort() == VendorSort.SLOT && this.mode == JournalMode.Type.NPC_SEARCH && JournalScreen.getSelectedNpc() != null;
  }

  private @Nullable JournalScreen getJournalScreen() {
    Screen p = this.parent;
    while (p instanceof RecipeScreen rs) {
      p = rs.getParent();
    }

    if (p instanceof JournalScreen journalScreen) {
      return journalScreen;
    }

    return null;
  }

  public enum VendorSort {
    A_TO_Z,
    SLOT,
  }

  public enum ItemSort {
    NONE,
    A_TO_Z,
    Z_TO_A,
  }
}
