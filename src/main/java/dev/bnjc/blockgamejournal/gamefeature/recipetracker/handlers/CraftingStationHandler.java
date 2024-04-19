package dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.RecipeTrackerGameFeature;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.station.CraftingStationItem;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.npc.NPCEntry;
import dev.bnjc.blockgamejournal.listener.interaction.SlotClickedListener;
import dev.bnjc.blockgamejournal.listener.screen.DrawSlotListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenOpenedListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenReceivedInventoryListener;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import dev.bnjc.blockgamejournal.util.NbtUtil;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CraftingStationHandler {
  private static final Logger LOGGER = BlockgameJournal.getLogger("Crafting Station");

  private static final Pattern COIN_PATTERN = Pattern.compile("([✔✖]) Requires (\\d+(?:\\.\\d+)?) Coin");
  private static final Pattern KNOWLEDGE_PATTERN = Pattern.compile("([✔✖]) Recipe Known");
  private static final Pattern CLASS_PATTERN = Pattern.compile("([✔✖]) Requires (\\d+) in ([A-Za-z]+)");
  private static final Pattern INGREDIENT_HEADER_PATTERN = Pattern.compile("Ingredients:");
  private static final Pattern INGREDIENT_PATTERN = Pattern.compile("([✔✖]) (\\d+) (.+?)$");

  private final RecipeTrackerGameFeature gameFeature;
  private final List<ItemStack> inventory = new ArrayList<>();

  private int syncId = -1;
  private String npcName = "";

  @Getter
  @Nullable
  private CraftingStationItem lastClickedItem;

  public CraftingStationHandler(RecipeTrackerGameFeature gameFeature) {
    this.gameFeature = gameFeature;
  }

  public void init() {
    ScreenOpenedListener.EVENT.register(this::handleOpenScreen);
    ScreenReceivedInventoryListener.EVENT.register(this::handleScreenInventory);
    SlotClickedListener.EVENT.register(this::handleSlotClicked);
    DrawSlotListener.EVENT.register(this::drawSlot);
  }

  private ActionResult handleOpenScreen(OpenScreenS2CPacket packet) {
    String screenName = packet.getName().getString();

    // Look for screen name "Some Name (#page#/#max#)" - exclude "Party" from the name
    Matcher matcher = Pattern.compile("^((?!Party)[\\w\\s]+)\\s\\(\\d+/\\d+\\)").matcher(screenName);
    PlayerEntity lastAttackedPlayer = this.gameFeature.getLastAttackedPlayer();
    if (matcher.find() ||
        (lastAttackedPlayer != null && screenName.equals(lastAttackedPlayer.getEntityName()))) {
      this.syncId = packet.getSyncId();

      if (lastAttackedPlayer != null) {
        this.npcName = lastAttackedPlayer.getEntityName();

        if (Journal.INSTANCE != null) {
          Journal.INSTANCE.getKnownNPCs().put(this.npcName, NPCEntry.of(lastAttackedPlayer));
        }
      } else {
        this.npcName = matcher.group(1);
      }
    } else {
      this.syncId = -1;
    }

    // TODO: Add paging
    this.inventory.clear();

    return ActionResult.PASS;
  }

  private ActionResult handleScreenInventory(InventoryS2CPacket packet) {
    if (packet.getSyncId() != this.syncId) {
      return ActionResult.PASS;
    }

    this.inventory.addAll(packet.getContents());
    return ActionResult.PASS;
  }

  private ActionResult handleSlotClicked(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player) {
    if (syncId != this.syncId) {
      return ActionResult.PASS;
    }

    if (slotId < 0 || slotId >= this.inventory.size()) {
      LOGGER.warn("[Blockgame Journal] Slot out of bounds: {}", slotId);
      return ActionResult.PASS;
    }

    ItemStack inventoryItem = this.inventory.get(slotId);

    if (inventoryItem == null || inventoryItem.isEmpty()) {
      LOGGER.warn("[Blockgame Journal] Empty item clicked");
      this.lastClickedItem = null;
      return ActionResult.PASS;
    }

    this.lastClickedItem = new CraftingStationItem(inventoryItem, slotId);

    // Log the NBT data of the clicked item
    NbtList loreTag = NbtUtil.getLore(inventoryItem);
    if (loreTag == null) {
      LOGGER.warn("[Blockgame Journal] No NBT lore data found");
      return ActionResult.PASS;
    }

    this.parseLoreMetadata(loreTag);
    return ActionResult.PASS;
  }

  private void drawSlot(DrawContext context, Slot slot) {
    if (this.syncId == -1 || this.inventory.isEmpty() || Journal.INSTANCE == null) {
      return;
    }

    ClientPlayerEntity player = MinecraftClient.getInstance().player;
    if (player != null && player.currentScreenHandler.syncId != this.syncId) {
      this.syncId = -1;
      this.inventory.clear();
      this.npcName = "";
      return;
    }

    boolean highlightMissing = BlockgameJournal.getConfig().getGeneralConfig().highlightMissingRecipes;
    boolean highlightOutdated = BlockgameJournal.getConfig().getGeneralConfig().highlightOutdatedRecipes;
    if (!highlightMissing && !highlightOutdated) {
      return;
    }

    // Check if slot is within bounds (0-53)
    if (slot.id < 0 || slot.id >= this.inventory.size() || slot.id >= 54) {
      return;
    }

    // See if slot item matches inventory item
    ItemStack slotItem = slot.getStack();
    ItemStack inventoryItem = this.inventory.get(slot.id);
    if (slotItem == null || inventoryItem == null || slotItem.isEmpty() || inventoryItem.isEmpty()) {
      return;
    }

    if (slotItem.getItem() != inventoryItem.getItem()) {
      LOGGER.warn("[Blockgame Journal] Slot item does not match inventory item");
      return;
    }

    List<JournalEntry> entries = Journal.INSTANCE.getEntries().getOrDefault(ItemUtil.getKey(inventoryItem), new ArrayList<>());
    for (JournalEntry entry : entries) {
      String expectedNpcName = entry.getNpcName();
      int expectedSlot = entry.getSlot();

      // If the item is in the journal, don't highlight it
      if (this.npcName.equals(expectedNpcName) && slot.id == expectedSlot) {
        if (highlightOutdated) {
          Optional<Integer> slotRevId = ItemUtil.getRevisionId(slotItem);
          if (slotRevId.isPresent() && slotRevId.get() != entry.getRevisionId()) {
            this.highlightSlot(context, slot, 0x30CCCC00);
          }
        }
        return;
      }
    }

    // If the item is not in the journal, highlight it
    if (highlightMissing) {
      this.highlightSlot(context, slot, 0x30FF0000);
    }
  }

  private void highlightSlot(DrawContext context, Slot slot, int color) {
    context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
  }

  /**
   * Parses the metadata from the clicked item's lore. This includes the coin cost, recipe known status, class
   * requirement, and expected ingredients.
   */
  private void parseLoreMetadata(NbtList loreTag) {
    if (this.lastClickedItem == null) {
      LOGGER.warn("[Blockgame Journal] No last clicked item found");
      return;
    }

    if (loreTag == null) {
      LOGGER.warn("[Blockgame Journal] No lore tag found");
      return;
    }

    boolean listingIngredients = false;
    for (int i = 0; i < loreTag.size(); i++) {
      String lore = loreTag.getString(i);
      MutableText text = NbtUtil.parseLoreLine(lore);
      if (text == null) {
        LOGGER.warn("[Blockgame Journal] Failed to parse lore line: {}", lore);
        continue;
      }

      lore = text.getString();

      if (listingIngredients) {
        Matcher ingredientMatcher = INGREDIENT_PATTERN.matcher(lore);
        if (ingredientMatcher.find()) {
          String name = ingredientMatcher.group(3);
          this.lastClickedItem.addExpectedIngredient(name, Integer.parseInt(ingredientMatcher.group(2)));
          continue;
        }

        if (lore.isBlank()) {
          // If we find an empty text, we might have more ingredients on the next line
          continue;
        }

        // If we didn't match an ingredient, we're done listing them
        listingIngredients = false;
      } else {
        // Check for coin cost
        Matcher coinMatcher = COIN_PATTERN.matcher(lore);
        if (coinMatcher.find()) {
          this.lastClickedItem.setCost(Float.parseFloat(coinMatcher.group(2)));
          continue;
        }

        // Check for recipe known
        Matcher knowledgeMatcher = KNOWLEDGE_PATTERN.matcher(lore);
        if (knowledgeMatcher.find()) {
          this.lastClickedItem.setRecipeKnown("✔".equals(knowledgeMatcher.group(1)) ? (byte) 1 : (byte) 0);
          continue;
        }

        // Check for class requirement
        Matcher classMatcher = CLASS_PATTERN.matcher(lore);
        if (classMatcher.find()) {
          this.lastClickedItem.setRequiredLevel(Integer.parseInt(classMatcher.group(2)));
          this.lastClickedItem.setRequiredClass(classMatcher.group(3));
          continue;
        }

        // Check for ingredients header
        Matcher ingredientMatcher = INGREDIENT_HEADER_PATTERN.matcher(lore);
        if (ingredientMatcher.find()) {
          listingIngredients = true;
          continue;
        }
      }

      LOGGER.debug("[Blockgame Journal] Unrecognized lore: {}", lore);
    }
  }
}
