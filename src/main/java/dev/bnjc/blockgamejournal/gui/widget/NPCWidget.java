package dev.bnjc.blockgamejournal.gui.widget;

import dev.bnjc.blockgamejournal.gui.screen.JournalScreen;
import dev.bnjc.blockgamejournal.journal.npc.NPCEntity;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class NPCWidget implements Drawable, Widget {
  private static final Identifier BACKGROUND_SPRITE = GuiUtil.sprite("background");

  @Setter
  private NPCEntity entity;
  private int x;
  private int y;
  private final int width;
  private final int height;

  public NPCWidget(NPCEntity entity, int x, int y, int width, int height) {
    this.entity = entity;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
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
      GuiUtil.drawMultiLineText(context, MinecraftClient.getInstance().textRenderer, this.x + 2,
          bottomY, npcTitleText, this.width - 4, 10, 0, 0x404040, false);
    }
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
  public int getHeight() {
    return this.height;
  }

  @Override
  public void forEachChild(Consumer<ClickableWidget> consumer) {
  }
}
