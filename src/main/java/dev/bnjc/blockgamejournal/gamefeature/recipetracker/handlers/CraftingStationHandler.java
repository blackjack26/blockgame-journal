package dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.RecipeTrackerGameFeature;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.station.CraftingStationItem;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.npc.NPCEntry;
import dev.bnjc.blockgamejournal.journal.npc.NPCUtil;
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
import net.minecraft.entity.Entity;
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
  private static final Pattern KNOWLEDGE_PATTERN = Pattern.compile("([✔✖]) Recipe (Known|Learned)");
  private static final Pattern CLASS_PATTERN = Pattern.compile("([✔✖]) Requires (\\d+) in ([A-Za-z]+)");
  private static final Pattern INGREDIENT_HEADER_PATTERN = Pattern.compile("Ingredients:");
  private static final Pattern INGREDIENT_PATTERN = Pattern.compile("([✔✖]) (\\d+) (.+?)$");

  private final RecipeTrackerGameFeature gameFeature;
  private final List<CraftingStationItem> inventory = new ArrayList<>();

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
    Entity lastAttackedEntity = this.gameFeature.getLastAttackedEntity();

    String entityName = "";
    if (lastAttackedEntity != null) {
      entityName = lastAttackedEntity.getEntityName();

      // Only use custom name if the entity is not a player
      if (!(lastAttackedEntity instanceof PlayerEntity) && lastAttackedEntity.hasCustomName()) {
        entityName = lastAttackedEntity.getCustomName().getString();
      }
    }

    if (matcher.find() || (lastAttackedEntity != null && screenName.equals(entityName))) {
      this.syncId = packet.getSyncId();

      if (lastAttackedEntity != null) {
        this.npcName = entityName;

        if (Journal.INSTANCE != null) {
          NPCUtil.createOrUpdate(this.npcName, lastAttackedEntity);
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

    packet.getContents().forEach(item -> {
      if (item == null || item.isEmpty()) {
        this.inventory.add(null);
        return;
      }

      CraftingStationItem invItem = new CraftingStationItem(item, this.inventory.size());
      NbtList loreTag = NbtUtil.getLore(invItem.getItem());
      if (loreTag != null) {
        this.parseLoreMetadata(invItem, loreTag);
      }

      this.inventory.add(invItem);
    });

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

    CraftingStationItem inventoryItem = this.inventory.get(slotId);

    if (inventoryItem == null) {
      LOGGER.warn("[Blockgame Journal] Empty item clicked");
      this.lastClickedItem = null;
      return ActionResult.PASS;
    }

    this.lastClickedItem = inventoryItem;
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
    CraftingStationItem inventoryItem = this.inventory.get(slot.id);
    if (slotItem == null || inventoryItem == null || slotItem.isEmpty()) {
      return;
    }

    if (slotItem.getItem() != inventoryItem.getItem().getItem()) {
      LOGGER.warn("[Blockgame Journal] Slot item does not match inventory item");
      return;
    }

    List<JournalEntry> entries = Journal.INSTANCE.getEntries().getOrDefault(ItemUtil.getKey(inventoryItem.getItem()), new ArrayList<>());
    for (JournalEntry entry : entries) {
      String expectedNpcName = entry.getNpcName();
      int expectedSlot = entry.getSlot();

      // If the item is in the journal, don't highlight it unless it's outdated
      if (this.npcName.equals(expectedNpcName) && slot.id == expectedSlot) {
        inventoryItem.setOutdated(ItemUtil.isOutdated(entry, inventoryItem));
        if (highlightOutdated && inventoryItem.getOutdated()) {
          this.highlightSlot(context, slot, 0x40CCCC00);
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
    context.drawBorder(slot.x, slot.y,  16, 16, color | 0xBB000000);
  }

  /**
   * Parses the metadata from the clicked item's lore. This includes the coin cost, recipe known status, class
   * requirement, and expected ingredients.
   */
  private void parseLoreMetadata(@Nullable CraftingStationItem item, NbtList loreTag) {
    if (item == null) {
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
          item.addExpectedIngredient(name, Integer.parseInt(ingredientMatcher.group(2)));
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
          item.setCost(Float.parseFloat(coinMatcher.group(2)));
          continue;
        }

        // Check for recipe known
        Matcher knowledgeMatcher = KNOWLEDGE_PATTERN.matcher(lore);
        if (knowledgeMatcher.find()) {
          item.setRecipeKnown("✔".equals(knowledgeMatcher.group(1)) ? (byte) 1 : (byte) 0);
          continue;
        }

        // Check for class requirement
        Matcher classMatcher = CLASS_PATTERN.matcher(lore);
        if (classMatcher.find()) {
          item.setRequiredLevel(Integer.parseInt(classMatcher.group(2)));
          item.setRequiredClass(classMatcher.group(3));
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
