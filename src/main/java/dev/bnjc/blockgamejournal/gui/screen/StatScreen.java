package dev.bnjc.blockgamejournal.gui.screen;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.statprofiles.PlayerAttribute;
import dev.bnjc.blockgamejournal.gamefeature.statprofiles.StatProfileGameFeature;
import dev.bnjc.blockgamejournal.gui.widget.stats.ModifierListWidget;
import dev.bnjc.blockgamejournal.gui.widget.stats.TalentListWidget;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import dev.bnjc.blockgamejournal.util.NbtUtil;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StatScreen extends Screen {
  public static final Logger LOGGER = BlockgameJournal.getLogger("Stat Screen");

  private static final Identifier BACKGROUND_SPRITE = GuiUtil.sprite("background");
  private static final ItemStack BOOK_ITEM = new ItemStack(Items.WRITABLE_BOOK);

  private static final int BUTTON_SIZE = 14;
  private static final int MENU_WIDTH = 224;
  private static final int MENU_HEIGHT = 224;
  private static final int TITLE_LEFT = 8;
  private static final int TITLE_TOP = 8;

  private final StatProfileGameFeature gameFeature;
  @Getter
  private final int syncId;

  private int left = 0;
  private int top = 0;

  @Getter
  private final Map<String, PlayerAttribute> attributes;
  private PlayerAttribute hoveredAttribute;

  /**
   * Temporary attributes that are being reallocated. This allows us to keep track of the changes before
   * they are confirmed.
   */
  private final Map<String, PlayerAttribute> tempAttributes;
  private int reallocateSlot = -1;
  private int availablePoints = -1;
  private boolean pointsFromClone = false;
  private int spentPoints = -1;

  private TalentListWidget listWidget;
  private TexturedButtonWidget reallocateButton;
  private ModifierListWidget modifierListWidget;

  public StatScreen(Text title, int syncId, StatProfileGameFeature gameFeature) {
    super(title);

    this.gameFeature = gameFeature;
    this.syncId = syncId;
    this.attributes = new HashMap<>();
    this.tempAttributes = new HashMap<>();
  }

  @Override
  protected void init() {
    this.left = (this.width - MENU_WIDTH) / 2;
    this.top = (this.height - MENU_HEIGHT) / 2;

    super.init();

    // List widget
    this.listWidget = new TalentListWidget(this, this.tempAttributes, this.left + 8, this.top + 40, MENU_WIDTH - 20, MENU_HEIGHT - 48);
    this.listWidget.visible = false;
    this.listWidget.setOnAttributeHover((attribute, hovered) -> {
      if (hovered) {
        this.hoveredAttribute = attribute;
      } else if (this.hoveredAttribute == attribute) {
        this.hoveredAttribute = null;
      }
    });
    this.addDrawableChild(this.listWidget);
    if (!this.tempAttributes.isEmpty()) {
      this.listWidget.build();
    }

    // Close button
    this.addDrawableChild(GuiUtil.close(
        this.left + MENU_WIDTH - (3 + BUTTON_SIZE),
        this.top + 5,
        button -> this.close()
    ));

    // Reallocation button
    this.reallocateButton = GuiUtil.button(
        this.left + MENU_WIDTH - 2 * (3 + BUTTON_SIZE),
        this.top + 5,
        "widgets/reallocate",
        "blockgamejournal.reallocate",
        (button) -> this.reallocateAttributes());
    this.reallocateButton.visible = this.reallocateSlot != -1;
    this.reallocateButton.setTooltip(Tooltip.of(Text.literal("Click to reallocate your attributes")));
    this.addDrawableChild(this.reallocateButton);

    // Modifier list widget
    this.modifierListWidget = new ModifierListWidget(this, this.left + MENU_WIDTH + 4, this.top, 175, MENU_HEIGHT);
    this.modifierListWidget.visible = false;
    this.addDrawableChild(this.modifierListWidget);
  }

  public void setInventory(@NotNull List<ItemStack> inventory) {
    this.attributes.clear();
    this.tempAttributes.clear();

    for (int slot = 0; slot < inventory.size(); slot++) {
      // Only check the first 44 slots (5 rows of 9 slots)
      if (slot >= 45) {
        break;
      }

      ItemStack itemStack = inventory.get(slot);

      // Check if the item is the "Reallocate Attributes" item
      if (itemStack.getName().getString().equals("Reallocate Attributes")) {
        this.reallocateSlot = slot;
        this.reallocateButton.visible = true;
        this.parseReallocationItem(itemStack);
        continue;
      }

      LOGGER.debug("Found item in slot {}: {}", slot, itemStack.getName().getString());
      PlayerAttribute attribute = PlayerAttribute.fromItem(itemStack, slot);
      if (attribute != null) {
        this.attributes.put(attribute.getName(), attribute);

        // If we haven't found the current points yet, try to find it
        if (this.availablePoints == -1 || this.pointsFromClone) {
          this.availablePoints = PlayerAttribute.getAvailablePoints(itemStack);
          this.pointsFromClone = false;
          LOGGER.debug("Found available points: {}", this.availablePoints);
        }
      }
    }

    this.tempAttributes.putAll(this.attributes);
    this.listWidget.build();
    this.modifierListWidget.build();
  }

  @Override
  public void close() {
    super.close();

    // Send a packet to the server saying we have closed the window
    if (this.client != null && this.client.player != null) {
      this.client.player.closeHandledScreen();
    }

    this.gameFeature.handleScreenClose();
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);

    // Title
    context.drawText(this.textRenderer, this.title, this.left + TITLE_LEFT, this.top + TITLE_TOP, 0x404040, false);

    // Centered Book Item
    context.drawItem(BOOK_ITEM, this.left + (MENU_WIDTH - 16) / 2, this.top + 8);

    // Centered below the book item, "Available: X/Y"
    int totalPoints = this.totalPoints();
    MutableText text = Text.literal("Available: ");
    if (totalPoints == -1) {
      text.append(Text.literal("?").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
    } else {
      text.append(Text.literal("" + this.availablePoints)
              .formatted(this.availablePoints > 0 ? Formatting.DARK_GREEN : Formatting.DARK_RED, Formatting.BOLD))
          .append(Text.literal("/" + totalPoints));
    }
    context.drawText(this.textRenderer, text, this.left + (MENU_WIDTH - this.textRenderer.getWidth(text)) / 2, this.top + 25, 0x404040, false);

    // Render tooltip
    this.renderTooltip(context, mouseX, mouseY);
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);

    // Background
    context.drawGuiTexture(BACKGROUND_SPRITE, this.left, this.top, MENU_WIDTH, MENU_HEIGHT);
  }

  public StatScreen clone(OpenScreenS2CPacket packet) {
    StatScreen screen = new StatScreen(packet.getName(), packet.getSyncId(), this.gameFeature);

    screen.attributes.putAll(this.attributes);
    screen.tempAttributes.putAll(this.tempAttributes);
    screen.availablePoints = this.availablePoints;
    screen.pointsFromClone = true;
    screen.spentPoints = this.spentPoints;
    screen.reallocateSlot = this.reallocateSlot;

    return screen;
  }

  private void renderTooltip(DrawContext context, int mouseX, int mouseY) {
    if (this.hoveredAttribute == null) {
      return;
    }

    // Render tooltip
    context.getMatrices().push();
    context.getMatrices().translate(0, 0, 200.0f);
    context.drawItemTooltip(MinecraftClient.getInstance().textRenderer, this.hoveredAttribute.getItemStack(), mouseX, mouseY);
    context.getMatrices().pop();
  }

  // region Actions

  private void reallocateAttributes() {
    ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
    if (interactionManager != null) {
      interactionManager.clickSlot(syncId, this.reallocateSlot, 0, SlotActionType.PICKUP, MinecraftClient.getInstance().player);
    }
  }

  // endregion Actions

  private void parseReallocationItem(ItemStack stack) {
    // Look for "You have spent a total of X attributes." in the lore
    NbtList loreList = NbtUtil.getLore(stack);
    if (loreList == null) {
      LOGGER.warn("No lore found for reallocation item");
      return;
    }

    for (int i = 0; i < loreList.size(); i++) {
      MutableText text = NbtUtil.parseLore(loreList, i);
      if (text == null) {
        continue;
      }

      String lore = text.getString();
      Pattern pattern = Pattern.compile("You have spent a total of (\\d+) attributes\\.");
      var matcher = pattern.matcher(lore);
      if (matcher.matches()) {
        this.spentPoints = Integer.parseInt(matcher.group(1));
        LOGGER.debug("Found spent points: {}", this.spentPoints);
        return;
      }
    }
  }

  private int totalPoints() {
    if (this.availablePoints == -1 || this.spentPoints == -1) {
      return -1;
    }
    return this.availablePoints + this.spentPoints;
  }
}
