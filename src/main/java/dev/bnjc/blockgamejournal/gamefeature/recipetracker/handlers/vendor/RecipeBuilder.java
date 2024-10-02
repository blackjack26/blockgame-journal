package dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers.vendor;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.station.CraftingStationItem;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.JournalEntryBuilder;
import dev.bnjc.blockgamejournal.journal.npc.NPCUtil;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RecipeBuilder {
  private static final int[] RECIPE_SLOTS = { 12, 13, 14, 21, 22, 23, 30, 31, 32 };

  private final List<ItemStack> ingredients;

  @Getter
  private boolean storedRecipe;

  @Setter
  private int page;

  private RecipeBuilder() {
    this.ingredients = new ArrayList<>();
    this.storedRecipe = false;
    this.page = 0;
  }

  public void goToNextPage() {
    this.page++;
  }

  public void goToPreviousPage() {
    this.page--;
  }

  public void addIngredientsFromPreview(List<ItemStack> previewItems) {
    // Only store the ingredients if they haven't been stored yet
    if (this.ingredients.size() - 1 < this.page * RECIPE_SLOTS.length) {
      // Get only the items in the recipe slots
      for (int slot : RECIPE_SLOTS) {
        ItemStack item = previewItems.get(slot);
        if (item.getItem() == Items.AIR) {
          continue;
        }

        this.ingredients.add(item);
      }
    } else {
      BlockgameJournal.LOGGER.debug("[Blockgame Journal] Ingredients already stored for page {}", page);
    }
  }

  public void updateKnownIngredients() {
    // Add or update all the known ingredients
    for (ItemStack stack : this.ingredients) {
      String ingredientKey = ItemUtil.getKey(stack);

      // Do not store "minecraft:" items
      if (!ingredientKey.startsWith("minecraft:")) {
        Journal.INSTANCE.getKnownItems().put(ingredientKey, stack);
      }

      BlockgameJournal.LOGGER.debug("[Blockgame Journal] - [ ] {} x{}", ItemUtil.getName(stack), stack.getCount());
    }
  }

  public boolean createEntry(
      @NotNull ItemStack item,
      @NotNull Entity vendor,
      @NotNull CraftingStationItem stationItem,
      String vendorName
  ) {
    if (Journal.INSTANCE == null) {
      BlockgameJournal.LOGGER.warn("[Blockgame Journal] Journal is not loaded");
      return false;
    }

    JournalEntry entry = new JournalEntryBuilder(this.ingredients, vendorName, stationItem.getSlot()).build(item);
    if (!this.validateEntry(stationItem, entry)) {
      BlockgameJournal.LOGGER.warn("[Blockgame Journal] Recipe validation failed");
      return false;
    }

    // Cost
    float cost = stationItem.getCost();
    entry.setCost(cost);
    if (cost != -1f) {
      BlockgameJournal.LOGGER.debug("[Blockgame Journal] - [ ] {} Coins", cost);
    }

    // Verify that there are at least some ingredients or a cost
    if (this.ingredients.isEmpty() && Float.compare(entry.getCost(), -1f) == 0) {
      BlockgameJournal.LOGGER.warn("[Blockgame Journal] No ingredients found in the recipe, and no cost was set");
      return false;
    }

    // Recipe Known
    Boolean recipeKnown = stationItem.getRecipeKnown();
    if (recipeKnown == null) {
      entry.setRecipeKnown((byte) -1);
    }
    else {
      entry.setRecipeKnown(recipeKnown ? (byte) 1 : (byte) 0);
      Journal.INSTANCE.getMetadata().setKnownRecipe(entry.getKey(), recipeKnown);
      BlockgameJournal.LOGGER.debug("[Blockgame Journal] - [{}] Recipe Known", recipeKnown ? "X" : " ");
    }

    // Class Requirement
    String requiredClass = stationItem.getRequiredClass();
    entry.setRequiredClass(requiredClass);
    if (requiredClass != null && !requiredClass.isEmpty()) {
      BlockgameJournal.LOGGER.debug("[Blockgame Journal] - [ ] Required Class: {}", requiredClass);
    }

    // Required Level
    int requiredLevel = stationItem.getRequiredLevel();
    entry.setRequiredLevel(requiredLevel);
    if (requiredLevel != -1) {
      BlockgameJournal.LOGGER.debug("[Blockgame Journal] - [ ] Required Level: {}", requiredLevel);
    }

    // Create or update the vendor
    if (!vendorName.isEmpty()) {
      NPCUtil.createOrUpdate(vendorName, vendor);
    }

    Journal.INSTANCE.addEntry(item, entry);
    this.storedRecipe = true;
    return true;
  }

  private boolean validateEntry(@NotNull CraftingStationItem item, JournalEntry entry) {
    // Validate the ingredients of the recipe
    BlockgameJournal.LOGGER.debug("[Blockgame Journal] Validating ingredients...");

    if (Journal.INSTANCE == null) {
      BlockgameJournal.LOGGER.warn("[Blockgame Journal] Journal is not loaded");
      return false;
    }

    // Validate the recipe keys match
    if (!ItemUtil.getKey(item.getItem()).equals(entry.getKey())) {
      BlockgameJournal.LOGGER.warn("[Blockgame Journal] Recipe key mismatch. Expected: {}, Actual: {}", ItemUtil.getKey(item.getItem()), entry.getKey());
      return false;
    }

    // Validate the expected ingredients
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
////        return false;
//        break;
//      }
//
//      String itemName = ItemUtil.getName(stack);
//      if (!item.getExpectedIngredients().containsKey(itemName)) {
//        BlockgameJournal.LOGGER.warn("[Blockgame Journal] Ingredient not expected: {}", itemName);
////        return false;
//        break;
//      }
//
//      int expectedAmount = item.getExpectedIngredients().get(itemName);
//      int actualAmount = entry.getIngredients().get(key);
//      if (expectedAmount != actualAmount) {
//        BlockgameJournal.LOGGER.warn("[Blockgame Journal] Ingredient amount mismatch: {} (expected: {}, actual: {})", key, expectedAmount, actualAmount);
////        return false;
//        break;
//      }
//    }

    return true;
  }

  public static RecipeBuilder create() {
    return new RecipeBuilder();
  }
}
