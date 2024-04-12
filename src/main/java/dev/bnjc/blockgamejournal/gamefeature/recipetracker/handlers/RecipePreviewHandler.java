package dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.RecipeTrackerGameFeature;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.JournalEntryBuilder;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.station.CraftingStationItem;
import dev.bnjc.blockgamejournal.listener.interaction.SlotClickedListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenOpenedListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenReceivedInventoryListener;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.KnowledgeBookItem;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RecipePreviewHandler {
  private static final Logger LOGGER = BlockgameJournal.getLogger("Recipe Preview");

  private static final int[] RECIPE_SLOTS = { 12, 13, 14, 21, 22, 23, 30, 31, 32 };

  private static final int BACK_BUTTON_INDEX = 10;
  private static final int ITEM_INDEX = 16;
  private static final int TRACK_BUTTON_INDEX = 19;
  private static final int PREV_PAGE_BUTTON_INDEX = 20;
  private static final int NEXT_PAGE_BUTTON_INDEX = 24;
  private static final int CRAFT_BUTTON_INDEX = 28;
  private static final int CONFIRM_BUTTON_INDEX = 34;

  private final RecipeTrackerGameFeature gameFeature;
  private int syncId = -1;

  @Getter
  private int recipePage = 0;

  private boolean isLoadingNextPage = false;
  private boolean isLoadingPrevPage = false;

  private final List<ItemStack> ingredients = new ArrayList<>();

  public RecipePreviewHandler(RecipeTrackerGameFeature gameFeature) {
    this.gameFeature = gameFeature;
  }

  public void init() {
    ScreenOpenedListener.EVENT.register(this::handleOpenScreen);
    ScreenReceivedInventoryListener.EVENT.register(this::handleScreenInventory);
    SlotClickedListener.EVENT.register(this::handleSlotClicked);
  }

  private ActionResult handleOpenScreen(OpenScreenS2CPacket packet) {
    String screenName = packet.getName().getString();

    if (screenName.equals("Recipe Preview")) {
      this.syncId = packet.getSyncId();

      // Reset the recipe page if the screen is not the same as the previous one
      if (!isLoadingNextPage && !isLoadingPrevPage) {
        recipePage = 0;
        ingredients.clear();
      }

      isLoadingNextPage = false;
      isLoadingPrevPage = false;
    } else {
      this.syncId = -1;
      this.reset();
    }

    return ActionResult.PASS;
  }

  private ActionResult handleScreenInventory(InventoryS2CPacket packet) {
    if (packet.getSyncId() != this.syncId) {
      return ActionResult.PASS;
    }

    List<ItemStack> inv = packet.getContents();
    boolean hasBackButton = inv.get(BACK_BUTTON_INDEX).getItem() instanceof PlayerHeadItem;
    boolean hasCraftButton = inv.get(CRAFT_BUTTON_INDEX).getItem() instanceof KnowledgeBookItem;
    boolean isScreenValid = hasBackButton && hasCraftButton;

    // Ensure that this is a valid recipe preview screen
    if (!isScreenValid) {
      return ActionResult.PASS;
    }

    this.storeRecipe(inv);

    // TODO: Add the track button to the inventory
//    packet.getContents().set(TRACK_BUTTON_INDEX, new TrackRecipeItem().getItemStack());
    return ActionResult.PASS;
  }

  private ActionResult handleSlotClicked(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player) {
    if (syncId != this.syncId) {
      return ActionResult.PASS;
    }

    // Handle track button click
    if (slotId == TRACK_BUTTON_INDEX) {
      this.handleTrackButtonClicked(button, actionType, player);
      return ActionResult.FAIL; // Prevent any further processing
    }

    // Handle next button click
    if (slotId == NEXT_PAGE_BUTTON_INDEX) {
      isLoadingNextPage = true;
      recipePage++;
    }
    // Handle prev button click
    else if (slotId == PREV_PAGE_BUTTON_INDEX) {
      isLoadingPrevPage = true;
      recipePage--;
    }

    return ActionResult.PASS;
  }

  public void reset() {
    this.recipePage = 0;
    this.ingredients.clear();
    this.clearLoading();
  }

  public void clearLoading() {
    this.isLoadingNextPage = false;
    this.isLoadingPrevPage = false;
  }

  private void storeRecipe(List<ItemStack> inventory) {
    if (Journal.INSTANCE == null) {
      LOGGER.warn("[Blockgame Journal] Journal is not loaded");
      return;
    }

    if (this.gameFeature.getLastAttackedPlayer() == null) {
      LOGGER.warn("[Blockgame Journal] No station entity to attribute the recipe to");
      return;
    }

    boolean hasPrevPageButton = inventory.get(PREV_PAGE_BUTTON_INDEX).getItem() instanceof PlayerHeadItem;
    boolean hasNextPageButton = inventory.get(NEXT_PAGE_BUTTON_INDEX).getItem() instanceof PlayerHeadItem;

    if (!hasPrevPageButton) {
      recipePage = 0;
    }

    ItemStack recipeItem = inventory.get(ITEM_INDEX);

    // Store the recipe in the player's journal
    BlockgameJournal.LOGGER.debug("[Blockgame Journal] Storing recipe for {}", ItemUtil.getName(recipeItem));

    // Only store the ingredients if they haven't been stored yet
    if (this.ingredients.size() - 1 < recipePage * RECIPE_SLOTS.length) {
      // Get only the items in the recipe slots
      for (int slot : RECIPE_SLOTS) {
        ItemStack item = inventory.get(slot);
        if (item.getItem() == Items.AIR) {
          continue;
        }

        this.ingredients.add(item);
      }
    } else {
      BlockgameJournal.LOGGER.debug("[Blockgame Journal] Ingredients already stored for page {}", recipePage);
    }

    if (this.ingredients.isEmpty()) {
      BlockgameJournal.LOGGER.warn("[Blockgame Journal] No ingredients found in the recipe");
      return;
    }

    if (!hasNextPageButton) {
      // Add or update all the known ingredients
      for (ItemStack stack : this.ingredients) {
        String ingredientKey = ItemUtil.getKey(stack);

        // Do not store "minecraft:" items
        if (!ingredientKey.startsWith("minecraft:")) {
          Journal.INSTANCE.getKnownItems().put(ingredientKey, stack);
        }

        BlockgameJournal.LOGGER.debug("[Blockgame Journal] - [ ] {} x{}", ItemUtil.getName(stack), stack.getCount());
      }

      CraftingStationItem lastClickedItem = this.gameFeature.getCraftingStationHandler().getLastClickedItem();
      JournalEntry entry = new JournalEntryBuilder(
          this.ingredients,
          this.gameFeature.getLastAttackedPlayer(),
          lastClickedItem == null ? -1 : lastClickedItem.getSlot()
      ).build(recipeItem);

      if (this.validateEntry(lastClickedItem, entry)) {
        if (lastClickedItem != null) {
          float cost = lastClickedItem.getCost();
          entry.setCost(cost);
          if (cost != -1f) {
            LOGGER.debug("[Blockgame Journal] - [ ] {} Coins", cost);
          }

          byte recipeKnown = lastClickedItem.getRecipeKnown();
          entry.setRecipeKnown(recipeKnown);
          if (recipeKnown != -1) {
            LOGGER.debug("[Blockgame Journal] - [{}] Recipe Known", recipeKnown == 1 ? "X" : " ");
          }

          String requiredClass = lastClickedItem.getRequiredClass();
          entry.setRequiredClass(requiredClass);
          if (requiredClass != null && !requiredClass.isEmpty()) {
            LOGGER.debug("[Blockgame Journal] - [ ] Required Class: {}", requiredClass);
          }

          int requiredLevel = lastClickedItem.getRequiredLevel();
          entry.setRequiredLevel(requiredLevel);
          if (requiredLevel != -1) {
            LOGGER.debug("[Blockgame Journal] - [ ] Required Level: {}", requiredLevel);
          }
        }
        Journal.INSTANCE.addEntry(recipeItem, entry);
      } else {
        BlockgameJournal.LOGGER.warn("[Blockgame Journal] Recipe validation failed");
      }
    } else {
      BlockgameJournal.LOGGER.info("[Blockgame Journal] Waiting for next page to store recipe");
    }
  }

  private boolean validateEntry(@Nullable CraftingStationItem item, JournalEntry entry) {
    // Validate the ingredients of the recipe
    BlockgameJournal.LOGGER.debug("[Blockgame Journal] Validating ingredients...");

    if (Journal.INSTANCE == null) {
      BlockgameJournal.LOGGER.warn("[Blockgame Journal] Journal is not loaded");
      return false;
    }

    // Validate we can from clicking the item
    if (item == null) {
      BlockgameJournal.LOGGER.warn("[Blockgame Journal] No last clicked item found");
      return false;
    }

    // Validate the recipe keys match
    if (!ItemUtil.getKey(item.getItem()).equals(entry.getKey())) {
      BlockgameJournal.LOGGER.warn("[Blockgame Journal] Recipe key mismatch. Expected: {}, Actual: {}", ItemUtil.getKey(item.getItem()), entry.getKey());
      return false;
    }

    // Validate the expected ingredients
    if (item.getExpectedIngredients().isEmpty()) {
      BlockgameJournal.LOGGER.warn("[Blockgame Journal] No expected ingredients found");
      return false;
    }

    if (entry.getIngredients().size() != item.getExpectedIngredients().size()) {
      BlockgameJournal.LOGGER.warn("[Blockgame Journal] Ingredient count mismatch");
      return false;
    }

    // TODO: Fix this matching
//    Set<String> ingredientKeys = entry.getIngredients().keySet();
//    for (String key : ingredientKeys) {
//      ItemStack stack = Journal.INSTANCE.getKnownItem(key);
//      if (stack == null) {
//        BlockgameJournal.LOGGER.warn("[Blockgame Journal] Ingredient not known: {}", key);
//        return false;
//      }
//
//      String itemName = ItemUtil.getName(stack);
//      if (!item.getExpectedIngredients().containsKey(itemName)) {
//        BlockgameJournal.LOGGER.warn("[Blockgame Journal] Ingredient not expected: {}", itemName);
//        return false;
//      }
//
//      int expectedAmount = item.getExpectedIngredients().get(itemName);
//      int actualAmount = entry.getIngredients().get(key);
//      if (expectedAmount != actualAmount) {
//        BlockgameJournal.LOGGER.warn("[Blockgame Journal] Ingredient amount mismatch: {} (expected: {}, actual: {})", key, expectedAmount, actualAmount);
//        return false;
//      }
//    }

    return true;
  }

  private void handleTrackButtonClicked(int button, SlotActionType actionType, PlayerEntity player) {
    // Handle tracking button click
    BlockgameJournal.LOGGER.info("[Blockgame Journal] Track button clicked!");
  }
}
