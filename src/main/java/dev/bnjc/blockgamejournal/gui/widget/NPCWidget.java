package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gui.screen.JournalScreen;
import dev.bnjc.blockgamejournal.journal.npc.NPCEntity;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class NPCWidget extends ClickableWidget {
  private static final Identifier BACKGROUND_SPRITE = GuiUtil.sprite("background");

  private NPCEntity entity;
  private int x;
  private int y;
  private final int width;
  private final int height;

  private final TexturedButtonWidget locateButton;
  private final TexturedButtonWidget stopLocateButton;

  public NPCWidget(NPCEntity entity, int x, int y, int width, int height) {
    super(x, y, width, height, Text.empty());

    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;

    this.locateButton = new TexturedButtonWidget(
        this.x + this.width - 16,
        this.y + 4,
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/locate/button"), GuiUtil.sprite("widgets/locate/button_highlighted")),
        (button) -> {
          this.entity.getNpcEntry().setLocating(true);
          updateButtons();
        }
    );

    this.stopLocateButton = new TexturedButtonWidget(
        this.x + this.width - 16,
        this.y + 4,
        12,
        12,
        new ButtonTextures(GuiUtil.sprite("widgets/stop_locate/button"), GuiUtil.sprite("widgets/stop_locate/button_highlighted")),
        (button) -> {
          this.entity.getNpcEntry().setLocating(false);
          updateButtons();
        }
    );

    setEntity(entity);
    updateButtons();
  }

  public void setEntity(@Nullable  NPCEntity entity) {
    this.entity = entity;
    updateButtons();
  }

  public void updateButtons() {
    this.locateButton.visible = entity != null && !entity.getNpcEntry().isLocating();
    this.stopLocateButton.visible = entity != null && entity.getNpcEntry().isLocating();

    if (this.entity != null) {
      this.locateButton.setTooltip(Tooltip.of(Text.literal("Locate " + this.entity.getNpcName().name() + " in the world.")));
      this.stopLocateButton.setTooltip(Tooltip.of(Text.literal("Stop locating " + this.entity.getNpcName().name() + " in the world.")));
    }
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    if (this.entity == null) {
      return;
    }

    context.drawGuiTexture(BACKGROUND_SPRITE, this.x, this.y, this.width, this.height);

    // Draw the NPC entity
    InventoryScreen.drawEntity(
        context,
        this.x + 2,
        this.y + 2,
        this.x + this.width - 2,
        this.y + this.height - 3,
        50,
        0.5f,
        mouseX,
        mouseY,
        this.entity
    );

    // Render the NPC name
    MutableText npcNameText = Text.literal(this.entity.getNpcName().name()).formatted(Formatting.WHITE);
    int bottomY = GuiUtil.drawMultiLineText(context, MinecraftClient.getInstance().textRenderer, this.x + 2,
        this.y + this.height + 2, npcNameText, this.width - 4, 10, 0, 0xFFFFFF, false);

    String npcTitle = this.entity.getNpcName().title();
    if (npcTitle != null) {
      MutableText npcTitleText = Text.literal(npcTitle).formatted(Formatting.GRAY);
      context.drawText(MinecraftClient.getInstance().textRenderer, npcTitleText, this.x + 2, bottomY, 0x404040, false);
    }

    // Render locate button
    if (this.entity.getNpcEntry().getPosition() != null) {
      this.stopLocateButton.render(context, mouseX, mouseY, delta);
      this.locateButton.render(context, mouseX, mouseY, delta);
    }
  }

  @Override
  public void onClick(double mouseX, double mouseY) {
    if (this.locateButton.visible) {
      this.locateButton.onClick(mouseX, mouseY);
    }
    else if (this.stopLocateButton.visible) {
      this.stopLocateButton.onClick(mouseX, mouseY);
    }
  }

  protected boolean clicked(double mouseX, double mouseY) {
    if (this.locateButton.visible) {
      return this.locateButton.isMouseOver(mouseX, mouseY);
    }

    if (this.stopLocateButton.visible) {
      return this.stopLocateButton.isMouseOver(mouseX, mouseY);
    }

    return super.clicked(mouseX, mouseY);
  }

  @Override
  public void setX(int x) {
    this.x = x;
  }

  @Override
  public void setY(int y) {
    this.y = y;
  }

  @Override
  public int getX() {
    return this.x;
  }

  @Override
  public int getY() {
    return this.y;
  }

  @Override
  public int getWidth() {
    return this.width;
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {

  }

  @Override
  public int getHeight() {
    return this.height;
  }

  @Override
  public void forEachChild(Consumer<ClickableWidget> consumer) {
  }
}
