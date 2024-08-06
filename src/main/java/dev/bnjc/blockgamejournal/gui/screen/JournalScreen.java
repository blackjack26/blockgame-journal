package dev.bnjc.blockgamejournal.gui.screen;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gui.widget.*;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.JournalItemStack;
import dev.bnjc.blockgamejournal.journal.JournalMode;
import dev.bnjc.blockgamejournal.journal.npc.NPCEntity;
import dev.bnjc.blockgamejournal.journal.recipe.JournalPlayerInventory;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import dev.bnjc.blockgamejournal.util.SearchUtil;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class JournalScreen extends Screen {
  public static final int GRID_SLOT_SIZE = 18;

  private static final Logger LOGGER = BlockgameJournal.getLogger("Recipe Journal");
  private static final Identifier BACKGROUND_SPRITE = GuiUtil.sprite("background");

  private static final int BUTTON_SIZE = 14;
  private static final int GRID_COLUMNS = 9;
  private static final int GRID_ROWS = 6;
  private static final int GRID_LEFT = 7;
  private static final int GRID_TOP = 45;
  private static final int MENU_WIDTH = 192;
  private static final int MENU_HEIGHT = 160;
  private static final int TITLE_LEFT = 8;
  private static final int TITLE_TOP = 8;
  private static final int SEARCH_LEFT = 6;
  private static final int SEARCH_TOP = 29;

  private static final int MODE_ICON_OFFSET = 24;
  private static final int MODE_ICON_SPACING = 24;

  private static @Nullable String lastSearch = null;

  @Getter
  private static @Nullable NPCEntity selectedNpc = null;

  @Getter
  private static @Nullable ItemStack selectedIngredient = null;

  @Getter
  private static ItemListWidget.VendorSort vendorItemSort = ItemListWidget.VendorSort.A_TO_Z;

  @Getter
  private static ItemListWidget.ItemSort itemSort = ItemListWidget.ItemSort.A_TO_Z;
  private static boolean useInventory = false;

  private int left = 0;
  private int top = 0;

  @Getter
  private JournalMode.Type currentMode;

  private final Screen parent;

  private final JournalPlayerInventory inventory;

  private TextFieldWidget search;
  private ItemListWidget itemList;
  private VerticalScrollWidget scroll;
  @Getter
  private TrackingWidget trackingWidget;
  private KnownRecipesWidget knownRecipesWidget;

  private ButtonWidget closeButton;
  private ButtonWidget itemSortButton;
  private ButtonWidget vendorSortButton;
  private ButtonWidget inventoryToggleOnButton;
  private ButtonWidget inventoryToggleOffButton;
  private NPCWidget npcWidget;

  private List<JournalItemStack> items = Collections.emptyList();

  public JournalScreen(@Nullable Screen parent) {
    super(Text.translatable("blockgamejournal.recipe_journal"));

    this.parent = parent;
    this.currentMode = BlockgameJournal.getConfig().getGeneralConfig().defaultMode;
    vendorItemSort = BlockgameJournal.getConfig().getGeneralConfig().defaultNpcSort;

    this.inventory = JournalPlayerInventory.defaultInventory();
  }

  @Override
  protected void init() {
    if (Journal.INSTANCE == null) {
      // TODO: Show error screen
      MinecraftClient.getInstance().setScreen(null);
      return;
    }

    this.left = (this.width - MENU_WIDTH) / 2;
    this.top = (this.height - MENU_HEIGHT) / 2;

    super.init();

    // Known Recipes
    int knownWidth = Math.min(200, this.width - (this.left + MENU_WIDTH + 4) - 4);
    this.knownRecipesWidget = new KnownRecipesWidget(
        this,
        this.width - knownWidth,
        0,
        knownWidth - 4,
        this.height
    );
    this.addDrawableChild(this.knownRecipesWidget);

    // Tracking
    this.trackingWidget = new TrackingWidget(
        this,
        0,
        0,
        left - MODE_ICON_OFFSET - 10,
        this.height
    );
    this.addDrawableChild(this.trackingWidget);
    this.refreshTracking();

    // Items
    this.itemList = new ItemListWidget(this, left + GRID_LEFT, top + GRID_TOP, GRID_COLUMNS, GRID_ROWS);

    // Scroll
    this.scroll = this.addDrawableChild(new VerticalScrollWidget(
        left + MENU_WIDTH - 19,
        top + GRID_TOP,
        this.itemList.getHeight(),
        Text.empty()
    ));
    this.scroll.setResponder(this.itemList::onScroll);

    this.addDrawableChild(this.itemList);

    // Search
    boolean shouldFocusSearch = this.search == null || this.search.isFocused();
//    shouldFocusSearch &= config.autofocusSearch;
    this.search = this.addDrawableChild(new SearchWidget(
        textRenderer,
        left + SEARCH_LEFT,
        top + SEARCH_TOP,
        MENU_WIDTH - (SEARCH_LEFT * 2),
        12,
        this.search,
        SearchWidget.SEARCH_MESSAGE
    ));
    this.search.setPlaceholder(SearchWidget.SEARCH_MESSAGE);
    this.search.setDrawsBackground(false);
    this.search.setChangedListener(this::filter);
    this.search.setEditableColor(0xFFFFFF);
    this.search.setText(JournalScreen.lastSearch);

    if (shouldFocusSearch) {
      this.setInitialFocus(this.search);
    }

    this.updateItems(this.search.getText());

    // Add warning if no journal
    if (Journal.INSTANCE == null) {
       this.search.setEditable(false);
       this.search.setText(I18n.translate("blockgamejournal.recipe_journal.no_journal"));
       this.search.setUneditableColor(0xFF4040);
    }

    // Close button
    this.closeButton = GuiUtil.close(
        this.left + MENU_WIDTH - (3 + BUTTON_SIZE),
        this.top + 5,
        button -> {
          this.setSelectedNpc(null);
          this.setSelectedIngredient(null);
          JournalScreen.useInventory = false;
        }
    );
    this.closeButton.visible = JournalScreen.selectedNpc != null || JournalScreen.selectedIngredient != null;
    this.addDrawableChild(this.closeButton);

    // Item sort button
    this.itemSortButton = new TexturedButtonWidget(
        this.left + MENU_WIDTH - (3 + BUTTON_SIZE),
        this.top + 5,
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/sort/button"), GuiUtil.sprite("widgets/sort/button_highlighted")),
        button -> {
          switch (JournalScreen.itemSort) {
            case NONE:
              JournalScreen.itemSort = ItemListWidget.ItemSort.A_TO_Z;
              break;
            case A_TO_Z:
              JournalScreen.itemSort = ItemListWidget.ItemSort.Z_TO_A;
              break;
            case Z_TO_A:
              JournalScreen.itemSort = ItemListWidget.ItemSort.NONE;
              break;
          }

          this.itemSortButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.sort." + JournalScreen.itemSort.name())));
          this.refreshItems();
        }
    );
    this.itemSortButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.sort." + JournalScreen.itemSort.name())));
    this.itemSortButton.visible = this.currentMode == JournalMode.Type.ITEM_SEARCH || this.currentMode == JournalMode.Type.FAVORITES;
    this.addDrawableChild(this.itemSortButton);

    // Inventory toggle on button
    this.inventoryToggleOnButton = new TexturedButtonWidget(
        this.left + MENU_WIDTH - 2 * (3 + BUTTON_SIZE),
        this.top + 5,
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/inventory/button_on"), GuiUtil.sprite("widgets/inventory/button_on_highlighted")),
        button -> {
          JournalScreen.useInventory = true;
          this.inventoryToggleOnButton.visible = false;
          this.inventoryToggleOffButton.visible = true;
          this.refreshItems();
        }
    );
    this.inventoryToggleOnButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.filter.inventory.false")));
    this.inventoryToggleOnButton.visible = (this.currentMode == JournalMode.Type.ITEM_SEARCH || this.currentMode == JournalMode.Type.FAVORITES) && !JournalScreen.useInventory;
    this.addDrawableChild(this.inventoryToggleOnButton);

    // Inventory toggle off button
    this.inventoryToggleOffButton = new TexturedButtonWidget(
        this.left + MENU_WIDTH - 2 * (3 + BUTTON_SIZE),
        this.top + 5,
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/inventory/button_off"), GuiUtil.sprite("widgets/inventory/button_off_highlighted")),
        button -> {
          JournalScreen.useInventory = false;
          this.inventoryToggleOnButton.visible = true;
          this.inventoryToggleOffButton.visible = false;
          this.refreshItems();
        }
    );
    this.inventoryToggleOffButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.filter.inventory.true")));
    this.inventoryToggleOffButton.visible = (this.currentMode == JournalMode.Type.ITEM_SEARCH || this.currentMode == JournalMode.Type.FAVORITES) && JournalScreen.useInventory;
    this.addDrawableChild(this.inventoryToggleOffButton);

    // Vendor sort button
    this.vendorSortButton = new TexturedButtonWidget(
        this.left + MENU_WIDTH - 2 * (3 + BUTTON_SIZE),
        this.top + 5,
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/sort/button"), GuiUtil.sprite("widgets/sort/button_highlighted")),
        button -> {
          JournalScreen.vendorItemSort = JournalScreen.vendorItemSort == ItemListWidget.VendorSort.A_TO_Z ? ItemListWidget.VendorSort.SLOT : ItemListWidget.VendorSort.A_TO_Z;
          this.vendorSortButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.sort." + JournalScreen.vendorItemSort.name())));
          this.refreshItems();
        }
    );
    this.vendorSortButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.sort." + JournalScreen.vendorItemSort.name())));
    this.vendorSortButton.visible = JournalScreen.selectedNpc != null;
    this.addDrawableChild(this.vendorSortButton);

    // NPC Widget
    this.npcWidget = new NPCWidget(JournalScreen.selectedNpc, this.left + MENU_WIDTH + 4, this.top, 68, 74);
    this.addDrawableChild(this.npcWidget);

    ///// Mode Buttons
    Map<JournalMode.Type, ModeButton> buttons = new HashMap<>();

    var modes = JournalMode.MODES.values().stream()
        .sorted(Comparator.comparing(JournalMode::order))
        .toList();

    for (int index = 0; index < modes.size(); index++) {
      JournalMode mode = modes.get(index);
      ItemStack stack = new ItemStack(mode.icon());

      ItemStack mmc = Journal.INSTANCE.getKnownNpcItem("Mayor McCheese");
      if (mode.type() == JournalMode.Type.NPC_SEARCH && mmc != null) {
        stack = mmc;
      }

      ModeButton modeButton = this.addDrawableChild(new ModeButton(
          stack,
          this.left - MODE_ICON_OFFSET,
          this.top + index * MODE_ICON_SPACING + 1,
          button -> {
            // Un-highlight old
            if (buttons.containsKey(this.currentMode)) {
              buttons.get(this.currentMode).setHighlighted(false);
            }

            // Highlight new
            this.currentMode = mode.type();
            buttons.get(this.currentMode).setHighlighted(true);

            this.setSelectedNpc(null);
            this.setSelectedIngredient(null);

            this.itemSortButton.visible = this.currentMode == JournalMode.Type.ITEM_SEARCH || this.currentMode == JournalMode.Type.FAVORITES;
            this.inventoryToggleOnButton.visible = this.itemSortButton.visible && !JournalScreen.useInventory;
            this.inventoryToggleOffButton.visible = this.itemSortButton.visible && JournalScreen.useInventory;
          }
      ));
      modeButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.mode." + mode.type().name())));
      buttons.put(mode.type(), modeButton);
    }

    // Mode button highlighting
    if (buttons.containsKey(this.currentMode)) {
      buttons.get(this.currentMode).setHighlighted(true);
    }
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);

    // Title
    Text titleText = this.title;
    if (JournalScreen.selectedNpc != null) {
      titleText = Text.translatable("blockgamejournal.recipe_journal.npc", JournalScreen.selectedNpc.getNpcName().name());
    } else if (JournalScreen.selectedIngredient != null) {
      titleText = Text.translatable("blockgamejournal.recipe_journal.ingredient", ItemUtil.getName(JournalScreen.selectedIngredient));
    } else if (this.currentMode == JournalMode.Type.FAVORITES) {
      titleText = Text.translatable("blockgamejournal.recipe_journal.favorites");
    } else if (this.currentMode == JournalMode.Type.NPC_SEARCH) {
      titleText = Text.translatable("blockgamejournal.recipe_journal.by_npc");
    } else if (this.currentMode == JournalMode.Type.INGREDIENT_SEARCH) {
      titleText = Text.translatable("blockgamejournal.recipe_journal.by_ingredient");
    }
    context.drawTextWrapped(
        textRenderer,
        titleText,
        this.left + TITLE_LEFT,
        this.top + TITLE_TOP,
        MENU_WIDTH - (TITLE_LEFT * 2) - BUTTON_SIZE - 2,
        0x404040
    );
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);

    // Background
    context.drawGuiTexture(BACKGROUND_SPRITE, this.left, this.top, MENU_WIDTH, MENU_HEIGHT);
  }

  @Override
  public void close() {
    JournalScreen.lastSearch = null; // Clear search
    JournalScreen.selectedNpc = null; // Clear selected NPC
    JournalScreen.selectedIngredient = null; // Clear selected ingredient
    super.close();
  }

  public void setSelectedNpc(String npc) {
    if (npc == null) {
      JournalScreen.selectedNpc = null;
    } else {
      JournalScreen.selectedNpc = new NPCEntity(MinecraftClient.getInstance().world, npc);
    }
    this.npcWidget.setEntity(JournalScreen.selectedNpc);

    this.search.setText("");
    JournalScreen.lastSearch = "";

    this.updateItems(null);
    this.closeButton.visible = npc != null;
    this.vendorSortButton.visible = npc != null;
  }

  public void setSelectedIngredient(ItemStack ingredient) {
    JournalScreen.selectedIngredient = ingredient;

    this.search.setText("");
    JournalScreen.lastSearch = "";

    this.updateItems(null);
    this.closeButton.visible = ingredient != null;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    boolean handled = this.itemList.keyPressed(keyCode, scanCode, modifiers);

    if (!handled) {
      handled = super.keyPressed(keyCode, scanCode, modifiers);
    }

    return handled;
  }

  @Override
  public boolean charTyped(char chr, int modifiers) {
    boolean handled = this.itemList.charTyped(chr, modifiers);

    if (!handled) {
      handled = super.charTyped(chr, modifiers);
    }

    return handled;
  }

  public void refreshItems() {
    this.updateItems(this.search.getText());
  }

  public void refreshTracking() {
    if (Journal.INSTANCE == null) {
      return;
    }

    this.trackingWidget.setEntries(Journal.INSTANCE.getEntries().values().stream()
        .flatMap(List::stream)
        .filter(JournalEntry::isTracked)
        .toList()
    );
  }

  private void updateItems(String filter) {
    if (Journal.INSTANCE == null) {
      return;
    }

    this.itemList.setMode(this.currentMode);

    if (this.currentMode == JournalMode.Type.ITEM_SEARCH) {
      this.showAllItems();
    }
    else if (this.currentMode == JournalMode.Type.NPC_SEARCH) {
      if (JournalScreen.selectedNpc == null) {
        this.showVendors();
      } else {
        this.showVendorItems();
      }
    }
    else if (this.currentMode == JournalMode.Type.FAVORITES) {
      this.showFavorites();
    }
    else if (this.currentMode == JournalMode.Type.INGREDIENT_SEARCH) {
      if (JournalScreen.selectedIngredient == null) {
        this.showIngredients();
      } else {
        this.showIngredientItems();
      }
    }
    else {
      this.items = Collections.emptyList();
    }
    filter(filter);
  }

  private void showFavorites() {
    if (Journal.INSTANCE == null) return;

    this.items = Journal.INSTANCE.getEntries().entrySet()
        .stream()
        .filter(entry -> entry.getValue().stream().anyMatch(e -> e.isFavorite() && this.inInventory(e)))
        .map(entry -> JournalItemStack.fromKnownItem(entry.getKey()))
        .filter(Objects::nonNull)

        .sorted((a, b) -> {
          if (JournalScreen.itemSort == ItemListWidget.ItemSort.A_TO_Z) {
            return a.getStack().getName().getString().compareToIgnoreCase(b.getStack().getName().getString());
          } else if (JournalScreen.itemSort == ItemListWidget.ItemSort.Z_TO_A) {
            return b.getStack().getName().getString().compareToIgnoreCase(a.getStack().getName().getString());
          }
          return 0;
        })
        .toList();
  }

  private void showAllItems() {
    if (Journal.INSTANCE == null) return;

    this.items = Journal.INSTANCE.getEntries().entrySet()
        .stream()
        .filter(entry -> entry.getValue().stream().anyMatch(this::inInventory))
        .map(entry -> JournalItemStack.fromKnownItem(entry.getKey()))
        .filter(Objects::nonNull)
        .sorted((a, b) -> {
          if (JournalScreen.itemSort == ItemListWidget.ItemSort.A_TO_Z) {
            return a.getStack().getName().getString().compareToIgnoreCase(b.getStack().getName().getString());
          } else if (JournalScreen.itemSort == ItemListWidget.ItemSort.Z_TO_A) {
            return b.getStack().getName().getString().compareToIgnoreCase(a.getStack().getName().getString());
          }
          return 0;
        })
        .toList();
  }

  private void showVendors() {
    if (Journal.INSTANCE == null) return;

    this.items = Journal.INSTANCE.getKnownNPCs().keySet()
        .stream()
        .map((key) -> {
          ItemStack s = Journal.INSTANCE.getKnownNpcItem(key);
          if (s != null) {
            return new JournalItemStack(s);
          }
          return null;
        })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(a -> a.getStack().getName().getString().toLowerCase(Locale.ROOT)))
        .toList();
  }

  private void showVendorItems() {
    if (Journal.INSTANCE == null) return;

    this.items = Journal.INSTANCE.getEntries().entrySet().stream()
        .filter(entry -> entry.getValue().stream().anyMatch(this::isNpcNameMatching))
        .map(entry -> {
          if (JournalScreen.vendorItemSort == ItemListWidget.VendorSort.A_TO_Z) {
            JournalItemStack stack = JournalItemStack.fromKnownItem(entry.getKey());
            if (stack != null) {
              return List.of(stack);
            }

            return List.<JournalItemStack>of();
          }

          return entry.getValue().stream()
              .filter(this::isNpcNameMatching)
              .map((e) -> new JournalItemStack(e.getItem(), e.getSlot()))
              .toList();
        })
        .flatMap(List::stream)
        .sorted(Comparator.comparing(a -> a.getStack().getName().getString().toLowerCase(Locale.ROOT)))
        .toList();
  }

  private void showIngredients() {
    if (Journal.INSTANCE == null) return;

    Set<String> ingredients = Journal.INSTANCE.getEntries().values().stream()
        .flatMap(List::stream)
        .flatMap(entry -> entry.getIngredients().keySet().stream())
        .collect(Collectors.toSet());

    this.items = ingredients.stream()
        .map(JournalItemStack::fromKnownItem)
        .filter(Objects::nonNull)
        .sorted((a, b) -> {
          if (JournalScreen.itemSort == ItemListWidget.ItemSort.A_TO_Z) {
            return a.getStack().getName().getString().compareToIgnoreCase(b.getStack().getName().getString());
          } else if (JournalScreen.itemSort == ItemListWidget.ItemSort.Z_TO_A) {
            return b.getStack().getName().getString().compareToIgnoreCase(a.getStack().getName().getString());
          }
          return 0;
        })
        .toList();
  }

  private void showIngredientItems() {
    if (Journal.INSTANCE == null) return;

    this.items = Journal.INSTANCE.getEntries().values().stream()
        .filter(journalEntries -> journalEntries.stream().anyMatch(this::hasIngredient))
        .map(journalEntries -> journalEntries.stream()
            .filter(this::hasIngredient)
            .map((e) -> new JournalItemStack(e.getItem(), e.getSlot()))
            .toList()
        )
        .flatMap(List::stream)
        .sorted(Comparator.comparing(a -> a.getStack().getName().getString().toLowerCase(Locale.ROOT)))
        .toList();
  }

  private void filter(@Nullable String filter) {
    JournalScreen.lastSearch = filter;

    List<JournalItemStack> filtered;
    if (filter == null || filter.isEmpty()) {
      filtered = this.items;
    } else {
      filtered = this.items.stream()
          .filter(item -> SearchUtil.defaultPredicate(item.getStack(), filter))
          .toList();
    }

    this.itemList.setItems(filtered);
    this.scroll.setDisabled(filtered.size() <= GRID_ROWS * GRID_COLUMNS);
  }

  private boolean isNpcNameMatching(JournalEntry entry) {
    if (JournalScreen.selectedNpc == null) {
      return false;
    }
    return entry.getNpcName().toLowerCase(Locale.ROOT).equals(selectedNpc.getNpcWorldName().toLowerCase(Locale.ROOT));
  }

  private boolean hasIngredient(JournalEntry entry) {
    if (JournalScreen.selectedIngredient == null) {
      return false;
    }
    return entry.getIngredients().containsKey(ItemUtil.getKey(selectedIngredient));
  }

  private boolean inInventory(JournalEntry entry) {
    // If not filtering by inventory, then always return true
    if (!JournalScreen.useInventory) return true;

    // If no ingredients, then it's not in inventory
    if (entry.getIngredients().isEmpty()) {
      return false;
    }

    boolean hasEnough = true;
    for (ItemStack item : entry.getIngredientItems()) {
      hasEnough &= this.inventory.neededCount(item) <= 0;
    }
    return hasEnough;
  }
}
