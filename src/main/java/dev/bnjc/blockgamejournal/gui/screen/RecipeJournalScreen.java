package dev.bnjc.blockgamejournal.gui.screen;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gui.widget.ItemListWidget;
import dev.bnjc.blockgamejournal.gui.widget.SearchWidget;
import dev.bnjc.blockgamejournal.gui.widget.VerticalScrollWidget;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import dev.bnjc.blockgamejournal.util.SearchUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class RecipeJournalScreen extends Screen {
  public static final int GRID_SLOT_SIZE = 18;

  private static final Logger LOGGER = BlockgameJournal.getLogger("Recipe Journal");
  private static final Identifier BACKGROUND_SPRITE = GuiUtil.sprite("background");

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

  private static @Nullable String lastSearch = null;

  private int left = 0;
  private int top = 0;

  private final Screen parent;
  private TextFieldWidget search;
  private ItemListWidget itemList;
  private VerticalScrollWidget scroll;

  private List<ItemStack> items = Collections.emptyList();

  public RecipeJournalScreen(@Nullable Screen parent) {
    super(Text.translatable("blockgamejournal.recipe_journal"));

    this.parent = parent;
  }

  @Override
  protected void init() {
//    if (Journal.INSTANCE == null) {
//      // TODO: Show error screen
//      MinecraftClient.getInstance().setScreen(null);
//      return;
//    }

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
    this.search.setText(RecipeJournalScreen.lastSearch);

    if (shouldFocusSearch) {
      this.setInitialFocus(this.search);
    }

    this.updateItems();

    // Add warning if no journal
    if (Journal.INSTANCE == null) {
       this.search.setEditable(false);
       this.search.setText(I18n.translate("blockgamejournal.recipe_journal.no_journal"));
       this.search.setUneditableColor(0xFF4040);
    }
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);

    // Title
    context.drawText(textRenderer, this.title, this.left + TITLE_LEFT, this.top + TITLE_TOP, 0x404040, false);
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);

    // Background
    context.drawGuiTexture(BACKGROUND_SPRITE, this.left, this.top, MENU_WIDTH, MENU_HEIGHT);
  }

  private void updateItems() {
    if (Journal.INSTANCE == null) {
      return;
    }

    this.items = Journal.INSTANCE.getEntries().keySet()
        .stream()
        .map(Journal.INSTANCE::getKnownItem)
        .filter(Objects::nonNull)
        .sorted(Comparator.<ItemStack, String>comparing(a -> a.getName().getString().toLowerCase(Locale.ROOT)).reversed())
        .toList();
    filter(null);
  }

  private void filter(@Nullable String filter) {
    RecipeJournalScreen.lastSearch = filter;

    List<ItemStack> filtered;
    if (filter == null || filter.isEmpty()) {
      filtered = this.items;
    } else {
      filtered = this.items.stream()
          .filter(item -> SearchUtil.defaultPredicate(item, filter))
          .toList();
    }

    this.itemList.setItems(filtered);
    this.scroll.setDisabled(filtered.size() <= GRID_ROWS * GRID_COLUMNS);
  }
}
