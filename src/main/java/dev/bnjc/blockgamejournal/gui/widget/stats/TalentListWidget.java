package dev.bnjc.blockgamejournal.gui.widget.stats;

import dev.bnjc.blockgamejournal.gamefeature.statprofiles.PlayerAttribute;
import dev.bnjc.blockgamejournal.gui.screen.StatScreen;
import dev.bnjc.blockgamejournal.gui.widget.ScrollableViewWidget;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class TalentListWidget extends ScrollableViewWidget {
  public static double lastScrollY = 0;

  private static final Map<String, int[]> ATTR_POSITIONS;
  static {
    ATTR_POSITIONS = new HashMap<>();

    // Offense
    ATTR_POSITIONS.put("Ferocity", new int[] { 0, 0 });
    ATTR_POSITIONS.put("Spartan", new int[] { 0, 1 });
    ATTR_POSITIONS.put("Dexterity", new int[] { 1, 0 });
    ATTR_POSITIONS.put("Alacrity", new int[] { 1, 1 });
    ATTR_POSITIONS.put("Intelligence", new int[] { 2, 0 });
    ATTR_POSITIONS.put("Precision", new int[] { 2, 1 });
    ATTR_POSITIONS.put("Assassin", new int[] { 3, 0 });
    ATTR_POSITIONS.put("Glass Cannon", new int[] { 3, 1 });
    ATTR_POSITIONS.put("Bravery", new int[] { 4, 0 });
    ATTR_POSITIONS.put("Bullying", new int[] { 4, 1 });
    ATTR_POSITIONS.put("Battleship", new int[] { 5, 0 });
    ATTR_POSITIONS.put("Dreadnought", new int[] { 5, 1 });

    // Support
    ATTR_POSITIONS.put("Wisdom", new int[] { 0, 0 });
    ATTR_POSITIONS.put("Pacifist", new int[] { 0, 1 });
    ATTR_POSITIONS.put("Volition", new int[] { 1, 0 });
    ATTR_POSITIONS.put("Somatics", new int[] { 1, 1 });
    ATTR_POSITIONS.put("Bloom", new int[] { 2, 0 });
    ATTR_POSITIONS.put("Time Lord", new int[] { 2, 1 });

    // Defense
    ATTR_POSITIONS.put("Tenacity", new int[] { 0, 0 });
    ATTR_POSITIONS.put("Shield Mastery", new int[] { 0, 1 });
    ATTR_POSITIONS.put("Fortress", new int[] { 1, 0 });
    ATTR_POSITIONS.put("Juggernaut", new int[] { 1, 1 });
    ATTR_POSITIONS.put("Beef Cake", new int[] { 2, 0 });
    ATTR_POSITIONS.put("Chunky Soup", new int[] { 2, 1 });
  }

  private final int ATTR_HEIGHT = 24;

  private final Map<String, PlayerAttribute> attributes;
  private final List<AttributeWidget> attributeWidgets;
  @Getter
  private final StatScreen parent;

  private int offenseStartY = 0;
  private int supportStartY = 0;
  private int defenseStartY = 0;
  private int otherStartY = -1;
  private int bottomY = 0;

  @Setter
  private BiConsumer<PlayerAttribute, Boolean> onAttributeHover;

  public TalentListWidget(StatScreen parent, Map<String, PlayerAttribute> attributes, int x, int y, int width, int height) {
    super(x, y, width, height, Text.empty());

    this.parent = parent;
    this.attributes = attributes;
    this.attributeWidgets = new ArrayList<>();
  }

  public void build() {
    this.attributeWidgets.clear();

    List<PlayerAttribute> offenseAttrs = new ArrayList<>();
    List<PlayerAttribute> defenseAttrs = new ArrayList<>();
    List<PlayerAttribute> supportAttrs = new ArrayList<>();
    List<PlayerAttribute> otherAttrs = new ArrayList<>();

    for (PlayerAttribute attr : this.attributes.values()) {
      switch (attr.getCategory()) {
        case OFFENSE -> offenseAttrs.add(attr);
        case DEFENSE -> defenseAttrs.add(attr);
        case SUPPORT -> supportAttrs.add(attr);
        default -> otherAttrs.add(attr);
      }
    }

    int y = this.getY() + 2;

    // Offense
    this.offenseStartY = y;
    y += 14; // Title
    this.buildAttributes(offenseAttrs, y, false);
    y += (int) Math.ceil(offenseAttrs.size() / 2f) * ATTR_HEIGHT;

    // Support
    y += 4; // Spacing
    this.supportStartY = y;
    y += 14; // Title
    this.buildAttributes(supportAttrs, y, false);
    y += (int) Math.ceil(supportAttrs.size() / 2f) * ATTR_HEIGHT;

    // Defense
    y += 4; // Spacing
    this.defenseStartY = y;
    y += 14; // Title
    this.buildAttributes(defenseAttrs, y, false);
    y += (int) Math.ceil(defenseAttrs.size() / 2f) * ATTR_HEIGHT;

    if (!otherAttrs.isEmpty()) {
      y += 4; // Spacing
      this.otherStartY = y;
      y += 14; // Title
      this.buildAttributes(otherAttrs, y, true);
      y += (int) Math.ceil(otherAttrs.size() / 2f) * ATTR_HEIGHT;
    }

    this.bottomY = y;
    this.visible = true;

    this.setScrollY(TalentListWidget.lastScrollY);
  }

  private void buildAttributes(List<PlayerAttribute> attrs, int startY, boolean other) {
    for (int i = 0; i < attrs.size(); i++) {
      PlayerAttribute attr = attrs.get(i);
      int[] pos = ATTR_POSITIONS.get(attr.getName());
      if (pos == null) {
        if (!other) {
          continue;
        }

        pos = new int[] { i / 2, i % 2 };
      }

      int attrX = this.getX() + pos[1] * this.getWidth() / 2;
      int attrY = startY + pos[0] * ATTR_HEIGHT;

      AttributeWidget widget = new AttributeWidget(this, attr, attrX, attrY, (this.getWidth() - this.getScrollbarWidth()) / 2, ATTR_HEIGHT - 2);
      widget.setOnHover((attribute, hovered) -> {
        if (this.onAttributeHover != null) {
          this.onAttributeHover.accept(attribute, hovered);
        }
      });
      this.attributeWidgets.add(widget);
    }
  }

  @Override
  protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!this.visible) {
      return;
    }

    // Render attributes
    for (AttributeWidget widget : this.attributeWidgets) {
      widget.render(context, mouseX, mouseY, delta);
    }

    // Render titles
    TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
    context.drawTextWithShadow(textRenderer, Text.literal("Offense").formatted(Formatting.BOLD), this.getX(), this.offenseStartY, 0xFFFFFF);
    context.drawTextWithShadow(textRenderer, Text.literal("Support").formatted(Formatting.BOLD), this.getX(), this.supportStartY, 0xFFFFFF);
    context.drawTextWithShadow(textRenderer, Text.literal("Defense").formatted(Formatting.BOLD), this.getX(), this.defenseStartY, 0xFFFFFF);

    if (this.otherStartY != -1) {
      context.drawText(textRenderer, Text.literal("Other").formatted(Formatting.BOLD), this.getX(), this.otherStartY, 0xFFFFFF, true);
    }
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (!this.visible) {
      return false;
    }

    for (AttributeWidget widget : this.attributeWidgets) {
      if (widget.mouseClicked(mouseX, mouseY, button)) {
        this.playDownSound(MinecraftClient.getInstance().getSoundManager());
        return true;
      }
    }

    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  protected int getContentsHeight() {
    return this.bottomY - this.getY();
  }

  @Override
  protected double getDeltaYPerScroll() {
    return 9.0;
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    // N/A
  }

  @Override
  protected void setScrollY(double scrollY) {
    super.setScrollY(scrollY);

    lastScrollY = scrollY;
  }
}
