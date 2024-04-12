package dev.bnjc.blockgamejournal.journal;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Getter
public class DecomposedJournalEntry {
  private final String key;
  private final int count;
  private float cost;
  private Map<String, Integer> ingredients;
  private Map<String, Integer> leftovers;
  private Set<String> npcNames;
  private Map<String, Integer> professions;
  private Map<String, Byte> knownRecipes;

  private ItemStack knownItem;

  public DecomposedJournalEntry(String key, int count) {
    this.key = key;
    this.count = count;
    this.cost = 0.0f;
    this.ingredients = new HashMap<>();
    this.leftovers = new HashMap<>();
    this.npcNames = new HashSet<>();
    this.professions = new HashMap<>();
    this.knownRecipes = new HashMap<>();
  }

  public @Nullable ItemStack getItem() {
    if (Journal.INSTANCE == null) {
      return null;
    }

    if (this.knownItem == null) {
      this.knownItem = Journal.INSTANCE.getKnownItem(this.key);
    }

    return this.knownItem;
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

    return items;
  }

  private static List<ItemStack> inventory;
  public static DecomposedJournalEntry decompose(JournalEntry entry) {
    // Populate inventory
    Entity entity = MinecraftClient.getInstance().getCameraEntity();
    if (entity instanceof ClientPlayerEntity player) {
      PlayerInventory inv = player.getInventory();
      inventory = new ArrayList<>();
      inventory.addAll(inv.main);
      inventory.addAll(inv.armor);
      inventory.addAll(inv.offHand);
    }

    DecomposedJournalEntry decomposed = new DecomposedJournalEntry(entry.getKey(), entry.getCount());
    parseJournalEntry(entry, decomposed, 1);
    return decomposed;
  }

  private static void parseJournalEntry(JournalEntry entry, DecomposedJournalEntry decomposed, int quantity) {
    if (Journal.INSTANCE == null) {
      return;
    }

    // Cost
    decomposed.cost += entry.getCost() * quantity;

    // NPC
    decomposed.npcNames.add(entry.getNpcName());

    // Required Profession
    if (entry.getRequiredClass() != null) {
      // Add the profession to the list
      // If the profession is already in the list, take the highest level
      decomposed.professions.merge(entry.getRequiredClass(), entry.getRequiredLevel(), Integer::max);
    }

    // Known Recipe
    if (entry.getRecipeKnown() != -1) {
      decomposed.knownRecipes.put(entry.getKey(), entry.getRecipeKnown());
    }

    // Ingredients
    for (ItemStack ingredient : entry.getIngredientItems()) {
      String ingredientKey = ItemUtil.getKey(ingredient);

      if (!ItemUtil.isFullyDecomposed(ingredientKey) && !inInventory(ingredient, quantity)) {
        List<JournalEntry> entries = Journal.INSTANCE.getEntries().get(ingredientKey);
        if (entries != null && !entries.isEmpty()) {
          // TODO: Handle multiple entries
          JournalEntry nextEntry = entries.get(0);
          if (!ItemUtil.isRecursiveRecipe(nextEntry, entry.getKey())) {
            int nextCount = nextEntry.getCount();
            int reqCount = ingredient.getCount() * quantity;
            parseJournalEntry(nextEntry, decomposed, (int) Math.ceil((double) reqCount / nextCount));
            continue;
          }
        }

        if (ingredientKey.startsWith("minecraft:") && BlockgameJournal.getConfig().getGeneralConfig().decomposeVanillaItems) {
          RecipeEntry<?> recipeEntry = ItemUtil.getRecipe(new Identifier(ingredientKey));
          parseVanillaRecipe(recipeEntry, ingredientKey, decomposed, ingredient.getCount());
          continue;
        }
      }

      // If there is no entry for the ingredient, add it to the list
      // If the ingredient is already in the list, add the count
      decomposed.ingredients.merge(ingredientKey, ingredient.getCount() * quantity, Integer::sum);
    }
  }

  private static void parseVanillaRecipe(RecipeEntry<?> recipeEntry, String key, DecomposedJournalEntry decomposed, int quantity) {
    if (recipeEntry != null && recipeEntry.value() instanceof CraftingRecipe recipe) {
      ItemStack result = ItemUtil.getOutput(recipe);
      List<Ingredient> mcIngredients = recipe.getIngredients();
      Map<String, Integer> recipeStacks = new HashMap<>();

      for (Ingredient ing : mcIngredients) {
        ItemStack[] stacks = ing.getMatchingStacks();

        // TODO: Handle multiple stacks
        if (stacks.length > 1) {
          BlockgameJournal.LOGGER.warn("Multiple stacks for ingredient: {}", key);
        }

        ItemStack stack = stacks.length > 0 ? stacks[0] : ItemStack.EMPTY;
        if (stack.isEmpty()) {
          continue;
        }

        recipeStacks.merge(ItemUtil.getKey(stack), stack.getCount(), Integer::sum);
      }

      int recipeCount = result.getCount();
      int recipeMultiplier = (int) Math.ceil((double) quantity / recipeCount);

      for (Map.Entry<String, Integer> recipeIngredient : recipeStacks.entrySet()) {
        RecipeEntry<?> re = ItemUtil.getRecipe(new Identifier(recipeIngredient.getKey()));

        // Prevent infinite recursion
        if (ItemUtil.isRecursiveRecipe(re, key)) {
          re = null;
        }
        parseVanillaRecipe(re, recipeIngredient.getKey(), decomposed, recipeIngredient.getValue() * recipeMultiplier);
      }
      return;
    }

    decomposed.ingredients.merge(key, quantity, Integer::sum);
  }

  private static boolean inInventory(ItemStack stack, int multiplier) {
    if (inventory == null || inventory.isEmpty()) {
      return false;
    }

    int requiredCount = stack.getCount() * multiplier;
    for (ItemStack invStack : inventory) {
      if (ItemUtil.isItemEqual(stack, invStack)) {
        requiredCount -= invStack.getCount();
      }
    }

    return requiredCount <= 0;
  }
}
