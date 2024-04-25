package dev.bnjc.blockgamejournal.gui.widget.stats;

import dev.bnjc.blockgamejournal.gamefeature.statprofiles.ModifierAggregate;
import dev.bnjc.blockgamejournal.gamefeature.statprofiles.ModifierType;
import dev.bnjc.blockgamejournal.gamefeature.statprofiles.PlayerAttribute;
import dev.bnjc.blockgamejournal.gamefeature.statprofiles.StatModifier;
import dev.bnjc.blockgamejournal.gui.screen.StatScreen;
import dev.bnjc.blockgamejournal.gui.widget.ScrollableViewWidget;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

public class ModifierListWidget extends ScrollableViewWidget {
  private static final Identifier BACKGROUND_SPRITE = GuiUtil.sprite("background");

  private final StatScreen parent;
  private int bottomY = 0;

  private final List<ModifierAggregate> offenseModifiers = new ArrayList<>();
  private final List<ModifierAggregate> defenseModifiers = new ArrayList<>();
  private final List<ModifierAggregate> unknownModifiers = new ArrayList<>();

  public ModifierListWidget(StatScreen parent, int x, int y, int width, int height) {
    super(x, y, width, height, Text.empty());

    this.parent = parent;
  }

  public void build() {
    this.offenseModifiers.clear();
    this.defenseModifiers.clear();
    this.unknownModifiers.clear();

    Map<String, ModifierAggregate> modifiers = new HashMap<>();

    for (PlayerAttribute attr : this.parent.getAttributes().values()) {
      for (StatModifier buff : attr.getBuffs()) {
        if (!modifiers.containsKey(buff.getName())) {
          modifiers.put(buff.getName(), new ModifierAggregate(buff.getName(), buff.getType(), buff.getCategory()));
        }

        modifiers.get(buff.getName()).addValue(buff.getValue() * attr.getSpent());
      }
    }

    for (ModifierAggregate modifier : modifiers.values()) {
      if (modifier.getValue() == 0) continue; // TODO: Maybe add config to show 0 value modifiers?

      switch (modifier.getCategory()) {
        case OFFENSE -> this.offenseModifiers.add(modifier);
        case DEFENSE -> this.defenseModifiers.add(modifier);
        default -> this.unknownModifiers.add(modifier);
      }
    }

    this.offenseModifiers.sort(Comparator.comparingInt(ModifierAggregate::getOrder));
    this.defenseModifiers.sort(Comparator.comparingInt(ModifierAggregate::getOrder));
    this.unknownModifiers.sort(Comparator.comparingInt(ModifierAggregate::getOrder));

    this.visible = true;
  }

  @Override
  protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
    if (offenseModifiers.isEmpty() && defenseModifiers.isEmpty() && unknownModifiers.isEmpty()) {
      return;
    }

    // Create a semi-transparent black background
    context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.bottomY + 4, 0x80_000000);

    int y = this.getY() + 6;
    int x = this.getX() + 4;
    int maxW = 0;
    TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

    // Offense Stats
    if (!offenseModifiers.isEmpty()) {
      MutableText titleText = Text.literal("Offense").formatted(Formatting.BOLD);
      maxW = Math.max(maxW, textRenderer.getWidth(titleText));

      context.drawTextWithShadow(textRenderer, titleText, x, y, 0xFFFFFF);
      y += 12;
      for (ModifierAggregate modifier : offenseModifiers) {
        MutableText modifierText = getModifierText(modifier);
        maxW = Math.max(maxW, textRenderer.getWidth(modifierText));

        context.drawText(textRenderer, modifierText, x, y, 0xFFFFFF, false);
        y += 12;
      }

      if (!defenseModifiers.isEmpty() || !unknownModifiers.isEmpty()) {
        y += 8;
      }
    }

    // Defense Stats
    if (!defenseModifiers.isEmpty()) {
      MutableText titleText = Text.literal("Defense").formatted(Formatting.BOLD);
      maxW = Math.max(maxW, textRenderer.getWidth(titleText));
      context.drawTextWithShadow(textRenderer, titleText, x, y, 0xFFFFFF);
      y += 12;

      for (ModifierAggregate modifier : defenseModifiers) {
        MutableText modifierText = getModifierText(modifier);
        maxW = Math.max(maxW, textRenderer.getWidth(modifierText));

        context.drawText(textRenderer, modifierText, x, y, 0xFFFFFF, false);
        y += 12;
      }

      if (!unknownModifiers.isEmpty()) {
        y += 8;
      }
    }

    // Other Stats
    if (!unknownModifiers.isEmpty()) {
      MutableText titleText = Text.literal("Other").formatted(Formatting.BOLD);
      maxW = Math.max(maxW, textRenderer.getWidth(titleText));
      context.drawTextWithShadow(textRenderer, titleText, x, y, 0xFFFFFF);
      y += 12;

      for (ModifierAggregate modifier : unknownModifiers) {
        MutableText modifierText = getModifierText(modifier);
        maxW = Math.max(maxW, textRenderer.getWidth(modifierText));

        context.drawText(textRenderer, modifierText, x, y, 0xFFFFFF, false);
        y += 12;
      }
    }

    this.bottomY = y;
    this.setWidth(maxW + 8);
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

  }

  private MutableText getModifierText(ModifierAggregate modifier) {
    double value = modifier.getValue();

    // Format the value to 2 decimal places at most.
    String valueStr = String.format("%.2f", value);

    // Remove trailing zeroes
    // Ex:
    // - 1.00 -> 1
    // - 1.50 -> 1.5
    // - 1.75 -> 1.75
    if (valueStr.contains(".") && valueStr.endsWith("0")) {
      valueStr = valueStr.replaceAll("0*$", "");

      // Remove the decimal point if there are no more digits after it.
      if (valueStr.endsWith(".")) {
        valueStr = valueStr.substring(0, valueStr.length() - 1);
      }
    }

    // Add a '%' sign if the modifier is relative.
    if (modifier.getType() == ModifierType.RELATIVE) {
      valueStr += "%";
    }

    // Add a '+' sign if the value is positive.
    if (value > 0) {
      valueStr = "+" + valueStr;
    }

    return Text.literal(modifier.getIcon() + " " + modifier.getDisplayName() + ": ")
        .append(Text.literal(valueStr).formatted(modifier.getValueColor()))
        .formatted(Formatting.GRAY);
  }
}
