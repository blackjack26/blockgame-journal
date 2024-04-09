package dev.bnjc.blockgamejournal.gui.screen;

import dev.bnjc.blockgamejournal.gui.widget.RecipeWidget;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.JournalEntryBuilder;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecipeDisplay extends Screen {
  private static final Identifier BACKGROUND_SPRITE = GuiUtil.sprite("background");

  private static final int BUTTON_SIZE = 14;
  private static final int MENU_WIDTH = 192;
  private static final int MENU_HEIGHT = 156;

  private final Screen parent;
  private final List<JournalEntry> entries;

  private int left = 0;
  private int top = 0;

  private int page = 0;
  private TexturedButtonWidget prevPageButton;
  private TexturedButtonWidget nextPageButton;
  private RecipeWidget recipeWidget;

  public RecipeDisplay(String key, Screen parent) {
    super(Text.empty());

    this.parent = parent;
    if (Journal.INSTANCE == null) {
      this.entries = Collections.emptyList();
    } else {
      this.entries = Journal.INSTANCE.getEntries().getOrDefault(key, new ArrayList<>());
    }
  }

  public RecipeDisplay(ItemStack stack, Screen parent) {
    this(JournalEntryBuilder.getKey(stack), parent);
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
        MENU_HEIGHT - 24
    );
    this.addDrawableChild(this.recipeWidget);
    this.recipeWidget.setEntry(this.entries.get(this.page));

    // Previous recipe button
    if (this.parent instanceof RecipeDisplay) {
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
    while (p instanceof RecipeDisplay) {
      p = ((RecipeDisplay) p).parent;
    }
    MinecraftClient.getInstance().setScreen(p);
  }

  private void goToPage(int page) {
    this.page = page;

    this.nextPageButton.visible = this.page < this.entries.size() - 1;
    this.prevPageButton.visible = this.page > 0;
    this.recipeWidget.setEntry(this.entries.get(this.page));
  }
}
