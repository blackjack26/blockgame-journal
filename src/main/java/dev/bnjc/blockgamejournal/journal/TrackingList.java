package dev.bnjc.blockgamejournal.journal;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.journal.recipe.JournalPlayerInventory;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

@Getter
public class TrackingList {
  private final List<JournalEntry> entries;

  private JournalPlayerInventory inventory;
  private boolean flattened;

  private float cost;
  private Map<String, Integer> ingredients;
  private Map<String, Integer> professions;
  private Map<String, Byte> knownRecipes;
  private Set<String> vendorNames;

  public TrackingList(List<JournalEntry> entries) {
    this.entries = entries;
    this.flattened = false;
    this.inventory = JournalPlayerInventory.defaultInventory();

    this.cost = 0.0f;
    this.ingredients = new HashMap<>();
    this.professions = new HashMap<>();
    this.knownRecipes = new HashMap<>();
    this.vendorNames = new HashSet<>();

    this.populateNested();
  }

  public void flatten() {
    if (this.flattened) {
      return;
    }

    this.populateFlattened();
    this.flattened = true;
  }

  public void nest() {
    if (!this.flattened) {
      return;
    }

    this.populateNested();
    this.flattened = false;
  }

  public List<ItemStack> getIngredientItems() {
    if (Journal.INSTANCE == null) {
      return List.of();
    }

    List<ItemStack> items = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : this.ingredients.entrySet()) {
      ItemStack item = null;

      if (entry.getKey().startsWith("mmoitems:")) {
        item = Journal.INSTANCE.getKnownItem(entry.getKey());
      }
      else if (entry.getKey().startsWith("minecraft:")) {
        item = new ItemStack(Registries.ITEM.get(new Identifier(entry.getKey())));
      }

      if (item != null) {
        item.setCount(entry.getValue());
        items.add(item);
      }
    }


    // Sort ingredients by name A-Z
    items.sort(Comparator.comparing(ItemUtil::getName));

    return items;
  }

  private void populateNested() {
    this.reset();

    for (JournalEntry entry : this.entries) {
      // Cost
      this.cost += entry.getCost();

      // Vendor
      this.vendorNames.add(entry.getNpcName());

      // Profession
      if (entry.getRequiredClass() != null) {
        // Add the profession to the list
        // If the profession is already in the list, take the highest level
        this.professions.merge(entry.getRequiredClass(), entry.getRequiredLevel(), Integer::max);
      }

      // Known Recipe
      if (entry.getRecipeKnown() != -1) {
        this.knownRecipes.put(entry.getKey(), entry.getRecipeKnown());
      }

      // Ingredients
      for (ItemStack ingredient : entry.getIngredientItems()) {
        this.ingredients.merge(ItemUtil.getKey(ingredient), ingredient.getCount(), Integer::sum);
      }
    }
  }

  private void populateFlattened() {
    this.reset();
    this.inventory.resetEditable();

    for (JournalEntry entry : this.entries) {
      this.parseFlattened(entry, 1);
    }
  }

  private void parseFlattened(JournalEntry entry, int quantity) {
    if (Journal.INSTANCE == null) {
      return;
    }

    // Cost
    this.cost += entry.getCost() * quantity;

    // Vendor
    this.vendorNames.add(entry.getNpcName());

    // Required Profession
    if (entry.getRequiredClass() != null) {
      // Add the profession to the list
      // If the profession is already in the list, take the highest level
      this.professions.merge(entry.getRequiredClass(), entry.getRequiredLevel(), Integer::max);
    }

    // Known Recipe
    if (entry.getRecipeKnown() != -1) {
      this.knownRecipes.put(entry.getKey(), entry.getRecipeKnown());
    }

    // Ingredients
    for (ItemStack ingredient : entry.getIngredientItems()) {
      String ingredientKey = ItemUtil.getKey(ingredient);

      int itemCount = ingredient.getCount() * quantity;
      int neededCount = this.inventory.consume(ingredient, itemCount);
      int inventoryCount = itemCount - neededCount;
      boolean hasEnough = neededCount <= 0;

      if (!ItemUtil.isFullyDecomposed(ingredientKey) && (!hasEnough || !BlockgameJournal.getConfig().getDecompositionConfig().partialDecomposition)) {
        List<JournalEntry> entries = Journal.INSTANCE.getEntries().get(ingredientKey);
        if (entries != null && !entries.isEmpty()) {
          // TODO: Handle multiple entries
          JournalEntry nextEntry = entries.get(0);
          if (!ItemUtil.isRecursiveRecipe(nextEntry, entry.getKey())) {
            int nextCount = nextEntry.getCount();
            int reqCount = itemCount - inventoryCount;

            if (inventoryCount > 0) {
              ingredients.merge(ingredientKey, inventoryCount, Integer::sum);
            }

            parseFlattened(nextEntry, (int) Math.ceil((double) reqCount / nextCount));
            continue;
          }
        }

        // TODO: Decompose vanilla items?
      }

      // If there is no entry for the ingredient, add it to the list
      // If the ingredient is already in the list, add the count
      ingredients.merge(ingredientKey, itemCount, Integer::sum);
    }
  }

  private void reset() {
    this.cost = 0.0f;
    this.ingredients = new HashMap<>();
    this.professions = new HashMap<>();
    this.knownRecipes = new HashMap<>();
    this.vendorNames = new HashSet<>();
  }
}
