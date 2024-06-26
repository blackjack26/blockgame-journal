package dev.bnjc.blockgamejournal.gui.screen;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gui.widget.*;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.JournalItemStack;
import dev.bnjc.blockgamejournal.journal.JournalMode;
import dev.bnjc.blockgamejournal.journal.npc.NPCEntity;
import dev.bnjc.blockgamejournal.util.GuiUtil;
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

public class JournalScreen extends Screen {
  public static final int GRID_SLOT_SIZE = 18;

  private static final Logger LOGGER = BlockgameJournal.getLogger("Recipe Journal");
  private static final Identifier BACKGROUND_SPRITE = GuiUtil.sprite("background");

  private static final int BUTTON_SIZE = 14;
  private static final int GRID_COLUMNS = 9;
  private static final int GRID_ROWS = 6;
  private static final int GRID_LEFT = 7;
  private static final int GRID_TOP = 41;
  private static final int MENU_WIDTH = 192;
  private static final int MENU_HEIGHT = 156;
  private static final int TITLE_LEFT = 8;
  private static final int TITLE_TOP = 8;
  private static final int SEARCH_LEFT = 6;
  private static final int SEARCH_TOP = 25;

  private static final int MODE_ICON_OFFSET = 24;
  private static final int MODE_ICON_SPACING = 24;

  private static @Nullable String lastSearch = null;

  @Getter
  private static @Nullable NPCEntity selectedNpc = null;

  @Getter
  private static ItemListWidget.Sort npcItemSort = ItemListWidget.Sort.A_TO_Z;

  private int left = 0;
  private int top = 0;

  @Getter
  private JournalMode.Type currentMode;

  private final Screen parent;

  private TextFieldWidget search;
  private ItemListWidget itemList;
  private VerticalScrollWidget scroll;

  private ButtonWidget closeButton;
  private ButtonWidget sortButton;
  private NPCWidget npcWidget;

  private List<JournalItemStack> items = Collections.emptyList();

  public JournalScreen(@Nullable Screen parent) {
    super(Text.translatable("blockgamejournal.recipe_journal"));

    this.parent = parent;
    this.currentMode = BlockgameJournal.getConfig().getGeneralConfig().defaultMode;
    npcItemSort = BlockgameJournal.getConfig().getGeneralConfig().defaultNpcSort;
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
        button -> this.setSelectedNpc(null)
    );
    this.closeButton.visible = JournalScreen.selectedNpc != null;
    this.addDrawableChild(this.closeButton);

    // Sort button
    this.sortButton = new TexturedButtonWidget(
        this.left + MENU_WIDTH - 2 * (3 + BUTTON_SIZE),
        this.top + 5,
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/sort/button"), GuiUtil.sprite("widgets/sort/button_highlighted")),
        button -> {
          JournalScreen.npcItemSort = JournalScreen.npcItemSort == ItemListWidget.Sort.A_TO_Z ? ItemListWidget.Sort.SLOT : ItemListWidget.Sort.A_TO_Z;
          this.sortButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.sort." + JournalScreen.npcItemSort.name())));
          this.refreshItems();
        }
    );
    this.sortButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.sort." + JournalScreen.npcItemSort.name())));
    this.sortButton.visible = JournalScreen.selectedNpc != null;
    this.addDrawableChild(this.sortButton);

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
    } else if (this.currentMode == JournalMode.Type.FAVORITES) {
      titleText = Text.translatable("blockgamejournal.recipe_journal.favorites");
    } else if (this.currentMode == JournalMode.Type.NPC_SEARCH) {
      titleText = Text.translatable("blockgamejournal.recipe_journal.by_npc");
    }
    context.drawText(textRenderer, titleText, this.left + TITLE_LEFT, this.top + TITLE_TOP, 0x404040, false);
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
    this.sortButton.visible = npc != null;
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

  private void updateItems(String filter) {
    if (Journal.INSTANCE == null) {
      return;
    }

    this.itemList.setMode(this.currentMode);

    if (this.currentMode == JournalMode.Type.ITEM_SEARCH) {
      // Filter journal entry items
      this.items = Journal.INSTANCE.getEntries().keySet()
          .stream()
          .map(JournalItemStack::fromKnownItem)
          .filter(Objects::nonNull)
          .sorted(Comparator.comparing(a -> a.getStack().getName().getString().toLowerCase(Locale.ROOT)))
          .toList();
    }
    else if (this.currentMode == JournalMode.Type.NPC_SEARCH) {
      // If filter matches "npc:Some Name", filter by recipes that have that NPC
      if (JournalScreen.selectedNpc != null) {
        this.items = Journal.INSTANCE.getEntries().entrySet().stream()
            .filter(entry -> entry.getValue().stream().anyMatch(this::isNpcNameMatching))
            .map(entry -> {
              if (JournalScreen.npcItemSort == ItemListWidget.Sort.A_TO_Z) {
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
      // Otherwise, show all known NPCs
      else {
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
    }
    else if (this.currentMode == JournalMode.Type.FAVORITES) {
      // Filter journal entry items
      this.items = Journal.INSTANCE.getEntries().entrySet()
          .stream()
          .filter(entry -> entry.getValue().stream().anyMatch(JournalEntry::isFavorite))
          .map(entry -> JournalItemStack.fromKnownItem(entry.getKey()))
          .filter(Objects::nonNull)
          .sorted(Comparator.comparing(a -> a.getStack().getName().getString().toLowerCase(Locale.ROOT)))
          .toList();
    }
    else {
      this.items = Collections.emptyList();
    }
    filter(filter);
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
}
