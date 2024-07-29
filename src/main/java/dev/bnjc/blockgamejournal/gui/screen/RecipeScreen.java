package dev.bnjc.blockgamejournal.gui.screen;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gui.widget.NPCWidget;
import dev.bnjc.blockgamejournal.gui.widget.RecipeWidget;
import dev.bnjc.blockgamejournal.journal.DecomposedJournalEntry;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.JournalMode;
import dev.bnjc.blockgamejournal.journal.npc.NPCEntity;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class RecipeScreen extends Screen {
  private static final Identifier BACKGROUND_SPRITE = GuiUtil.sprite("background");

  private static final int BUTTON_SIZE = 14;
  private static final int MENU_WIDTH = 192;
  private static final int MENU_HEIGHT = 156;

  private final String key;
  @Getter
  private final Screen parent;
  private List<JournalEntry> entries;

  private int left = 0;
  private int top = 0;

  private int page = 0;
  private TexturedButtonWidget prevPageButton;
  private TexturedButtonWidget nextPageButton;
  private RecipeWidget recipeWidget;
  private TexturedButtonWidget decomposeButton;
  private TexturedButtonWidget favoriteButton;
  private TexturedButtonWidget unfavoriteButton;
  private TexturedButtonWidget trackButton;
  private TexturedButtonWidget untrackButton;
  private NPCWidget npcWidget;

  public RecipeScreen(String key, Screen parent) {
    super(Text.empty());

    this.key = key;
    this.parent = parent;
    this.filterEntries(null);
  }

  public RecipeScreen(ItemStack stack, Screen parent) {
    this(ItemUtil.getKey(stack), parent);
  }

  public void filterEntries(@Nullable Predicate<JournalEntry> predicate) {
    if (predicate == null) {
      if (Journal.INSTANCE == null) {
        this.entries = Collections.emptyList();
      } else {
        this.entries = Journal.INSTANCE.getEntries().getOrDefault(key, new ArrayList<>());
      }
    } else {
      this.entries = this.entries.stream().filter(predicate).toList();
    }
  }

  @Override
  protected void init() {
    this.left = (this.width - MENU_WIDTH) / 2;
    this.top = (this.height - MENU_HEIGHT) / 2;

    super.init();

    JournalEntry currentEntry = this.entries.get(this.page);

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
        new ButtonTextures(GuiUtil.sprite("widgets/paging/next_button"), GuiUtil.sprite("widgets/paging/next_button_highlighted")),
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
        new ButtonTextures(GuiUtil.sprite("widgets/paging/prev_button"), GuiUtil.sprite("widgets/paging/prev_button_highlighted")),
        button -> this.goToPage(this.page - 1)
    );
    this.prevPageButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.prev_page")));
    this.prevPageButton.visible = this.page > 0;
    this.addDrawableChild(this.prevPageButton);

    // Recipe widget
    this.recipeWidget = new RecipeWidget(
        this,
        this.left + 8,
        this.top + 10,
        MENU_WIDTH - 16,
        MENU_HEIGHT - 28
    );
    this.addDrawableChild(this.recipeWidget);
    this.recipeWidget.setEntry(currentEntry);

    // Previous recipe button
    if (this.parent instanceof RecipeScreen) {
      TexturedButtonWidget prevRecipeButton = new TexturedButtonWidget(
          this.left + 5,
          this.top + 5,
          12,
          12,
          new ButtonTextures(GuiUtil.sprite("widgets/paging/prev_button"), GuiUtil.sprite("widgets/paging/prev_button_highlighted")),
          button -> MinecraftClient.getInstance().setScreen(this.parent)
      );
      prevRecipeButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.prev_recipe")));
      this.addDrawableChild(prevRecipeButton);
    }

    // Remove the recipe from the journal (Bottom left)
    TexturedButtonWidget removeButton = new TexturedButtonWidget(
        this.left + 5,
        this.top + MENU_HEIGHT - (3 + BUTTON_SIZE),
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/remove/button"), GuiUtil.sprite("widgets/remove/button_highlighted")),
        button -> {
          if (Journal.INSTANCE == null) {
            return;
          }

          Journal.INSTANCE.removeEntry(this.key, this.page);
          this.entries = Journal.INSTANCE.getEntries().get(this.key);
          if (this.entries == null) {
            this.close();
            return;
          }

          this.goToPage(Math.min(this.page, this.entries.size() - 1));
        }
    );
    removeButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.remove_recipe")));
    this.addDrawableChild(removeButton);

    // Decompose the recipe (next to close button)
    this.decomposeButton = new TexturedButtonWidget(
        this.left + MENU_WIDTH - 2 * (3 + BUTTON_SIZE),
        this.top + 5,
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/decompose/button"), GuiUtil.sprite("widgets/decompose/button_highlighted")),
        button -> {
          if (Journal.INSTANCE == null) {
            return;
          }

          DecomposedJournalEntry decomposed = this.entries.get(this.page).decompose();

          boolean showNpc = this.parent instanceof JournalScreen journalScreen && journalScreen.getCurrentMode() == JournalMode.Type.NPC_SEARCH;
          MinecraftClient.getInstance().setScreen(new DecompositionScreen(decomposed, this, showNpc));
        }
    );
    this.decomposeButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.decompose_recipe")));
    this.decomposeButton.visible = this.hasDecomposableIngredients();
    this.addDrawableChild(this.decomposeButton);

    // Favorite button
    this.favoriteButton = new TexturedButtonWidget(
        this.decomposeButton.visible ? this.decomposeButton.getX() - (3 + BUTTON_SIZE) : this.decomposeButton.getX(),
        this.top + 5,
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/favorite/button"), GuiUtil.sprite("widgets/favorite/button_highlighted")),
        button -> {
          if (Journal.INSTANCE == null) {
            return;
          }

          JournalEntry entry = this.entries.get(this.page);
          entry.setFavorite(true);
          this.updateButtons();
        }
    );
    this.favoriteButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.favorite_recipe")));
    this.favoriteButton.visible = !currentEntry.isFavorite();
    this.addDrawableChild(this.favoriteButton);

    // Unfavorite button
    this.unfavoriteButton = new TexturedButtonWidget(
        this.decomposeButton.visible ? this.decomposeButton.getX() - (3 + BUTTON_SIZE) : this.decomposeButton.getX(),
        this.top + 5,
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/unfavorite/button"), GuiUtil.sprite("widgets/unfavorite/button_highlighted")),
        button -> {
          if (Journal.INSTANCE == null) {
            return;
          }

          JournalEntry entry = this.entries.get(this.page);
          entry.setFavorite(false);
          this.updateButtons();
        }
    );
    this.unfavoriteButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.unfavorite_recipe")));
    this.unfavoriteButton.visible = currentEntry.isFavorite();
    this.addDrawableChild(this.unfavoriteButton);

    // Track button
    this.trackButton = new TexturedButtonWidget(
        this.decomposeButton.visible ? this.decomposeButton.getX() - 2 * (3 + BUTTON_SIZE) : this.decomposeButton.getX() - (3 + BUTTON_SIZE),
        this.top + 5,
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/locate/button"), GuiUtil.sprite("widgets/locate/button_highlighted")),
        button -> {
          if (Journal.INSTANCE == null) {
            return;
          }

          JournalEntry entry = this.entries.get(this.page);
          entry.setTracked(true);

          JournalScreen js = this.getJournalScreen();
          if (js != null) {
            js.refreshTracking();
          }

          this.updateButtons();
        }
    );
    this.trackButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.track_recipe")));
    this.trackButton.visible = !currentEntry.isTracked();
    this.addDrawableChild(this.trackButton);

    // Untrack button
    this.untrackButton = new TexturedButtonWidget(
        this.decomposeButton.visible ? this.decomposeButton.getX() - 2 * (3 + BUTTON_SIZE) : this.decomposeButton.getX() - (3 + BUTTON_SIZE),
        this.top + 5,
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/stop_locate/button"), GuiUtil.sprite("widgets/stop_locate/button_highlighted")),
        button -> {
          if (Journal.INSTANCE == null) {
            return;
          }

          JournalEntry entry = this.entries.get(this.page);
          entry.setTracked(false);

          JournalScreen js = this.getJournalScreen();
          if (js != null) {
            js.refreshTracking();
          }

          this.updateButtons();
        }
    );
    this.untrackButton.setTooltip(Tooltip.of(Text.translatable("blockgamejournal.untrack_recipe")));
    this.untrackButton.visible = currentEntry.isTracked();
    this.addDrawableChild(this.untrackButton);

    // NPC Widget
    this.npcWidget = new NPCWidget(null, this.left + MENU_WIDTH + 4, this.top, 68, 74);
    this.addDrawableChild(this.npcWidget);
    if (this.parent instanceof JournalScreen journalScreen && journalScreen.getCurrentMode() == JournalMode.Type.NPC_SEARCH) {
      this.npcWidget.setEntity(JournalScreen.getSelectedNpc());
    }

    // Tracking widget
    JournalScreen js = this.getJournalScreen();
    if (js != null) {
      this.addDrawableChild(js.getTrackingWidget());
    }

    this.goToPage(this.page);
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

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);

    // Background
    context.drawGuiTexture(BACKGROUND_SPRITE, this.left, this.top, MENU_WIDTH, MENU_HEIGHT);
  }

  @Override
  public void close() {
    // Go back to the Journal screen
    Screen p = this.parent;
    while (p instanceof RecipeScreen) {
      p = ((RecipeScreen) p).parent;
    }
    MinecraftClient.getInstance().setScreen(p);
  }

  private void goToPage(int page) {
    this.page = page;

    this.updateButtons();

    JournalEntry entry = this.entries.get(this.page);
    this.recipeWidget.setEntry(entry);

    if (entry != null) {
      NPCEntity entity = new NPCEntity(MinecraftClient.getInstance().world, entry.getNpcName());
      this.npcWidget.setEntity(entity);
    } else {
      this.npcWidget.setEntity(null);
    }
  }

  private void updateButtons() {
    this.nextPageButton.visible = this.page < this.entries.size() - 1;
    this.prevPageButton.visible = this.page > 0;

    this.decomposeButton.visible = this.hasDecomposableIngredients();
    this.favoriteButton.visible = !this.entries.get(this.page).isFavorite();
    this.unfavoriteButton.visible = !this.favoriteButton.visible;
    this.trackButton.visible = !this.entries.get(this.page).isTracked();
    this.untrackButton.visible = !this.trackButton.visible;

    int favX = this.decomposeButton.visible ? this.decomposeButton.getX() - (3 + BUTTON_SIZE) : this.decomposeButton.getX();
    this.favoriteButton.setX(favX);
    this.unfavoriteButton.setX(favX);
    this.trackButton.setX(favX - (3 + BUTTON_SIZE));
    this.untrackButton.setX(favX - (3 + BUTTON_SIZE));
  }

  private boolean hasDecomposableIngredients() {
    if (Journal.INSTANCE == null) {
      return false;
    }

    JournalEntry entry = this.entries.get(this.page);
    for (String ingredient : entry.getIngredients().keySet()) {
      if (ItemUtil.isFullyDecomposed(ingredient)) {
        continue;
      }

      if (ingredient.startsWith("mmoitems:")) {
        JournalEntry nextEntry = Journal.INSTANCE.getFirstJournalEntry(ingredient);
        if (nextEntry != null && !ItemUtil.isRecursiveRecipe(nextEntry, entry.getKey())) {
          return true;
        }
      }
      else if (ingredient.startsWith("minecraft:") && !ItemUtil.isFullyDecomposed(ingredient) && BlockgameJournal.getConfig().getDecompositionConfig().decomposeVanillaItems) {
        Identifier id = new Identifier(ingredient);
        RecipeEntry<?> recipeEntry = ItemUtil.getRecipe(id);
        if (recipeEntry != null && !ItemUtil.isRecursiveRecipe(recipeEntry, entry.getKey())) {
          return true;
        }
      }
    }

    return false;
  }
}
