package dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.RecipeTrackerGameFeature;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.station.CraftingStationItem;
import dev.bnjc.blockgamejournal.util.NbtUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
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

  @Getter
  @Setter
  private int syncId = -1;

  @Getter
  @Nullable
  private CraftingStationItem lastClickedItem;

  public CraftingStationHandler(RecipeTrackerGameFeature gameFeature) {
    this.gameFeature = gameFeature;
  }

  public ActionResult handleOpenScreen(OpenScreenS2CPacket packet) {
    this.syncId = packet.getSyncId();

    // TODO: Add paging
    this.inventory.clear();

    return ActionResult.PASS;
  }

  public ActionResult handleScreenInventory(InventoryS2CPacket packet) {
    if (packet.getSyncId() != this.syncId) {
      return ActionResult.PASS;
    }

    this.inventory.addAll(packet.getContents());
    return ActionResult.PASS;
  }

  public ActionResult handleSlotClicked(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player) {
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

    this.lastClickedItem = new CraftingStationItem(inventoryItem);

    // Log the NBT data of the clicked item
    NbtList loreTag = NbtUtil.getLore(inventoryItem);
    if (loreTag == null) {
      LOGGER.warn("[Blockgame Journal] No NBT lore data found");
      return ActionResult.PASS;
    }

    this.parseLoreMetadata(loreTag);
    return ActionResult.PASS;
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

      LOGGER.info("[Blockgame Journal] Unrecognized lore: {}", lore);
    }
  }
}
