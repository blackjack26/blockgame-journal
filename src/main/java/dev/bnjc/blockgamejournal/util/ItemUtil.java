package dev.bnjc.blockgamejournal.util;

import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public class ItemUtil {
  /**
   * Items that have recipes but should be ignored when decomposing items.
   */
  private static final Set<String> BASE_ITEMS = Set.of(
      "minecraft:coal",
      "minecraft:copper_ingot",
      "minecraft:lapis_lazuli",
      "minecraft:redstone",
      "mmoitems:ESSENCE_CORRUPTED",
      "mmoitems:ESSENCE_LIFE",
      "mmoitems:ESSENCE_WATER",
      "mmoitems:ESSENCE_WIND",
      "mmoitems:ESSENCE_FIRE",
      "mmoitems:ESSENCE_EARTH"
  );

  public static String getKey(ItemStack itemStack) {
    String key = Registries.ITEM.getId(itemStack.getItem()).toString();

    // If result has "MMOITEMS_ITEM_ID" tag, then it is an MMOItems item. Otherwise, it is a vanilla item.
    NbtCompound stackNbt = itemStack.getNbt();
    if (stackNbt != null && stackNbt.contains("MMOITEMS_ITEM_ID")) {
      key = "mmoitems:" + stackNbt.getString("MMOITEMS_ITEM_ID");
    }

    return key;
  }

  public static String getName(ItemStack itemStack) {
    String name = itemStack.getName().getString();

    // If result has "MMOITEMS_NAME" tag, then it is an MMOItems item. Otherwise, it is a vanilla item.
    NbtCompound stackNbt = itemStack.getNbt();
    if (stackNbt != null && stackNbt.contains("MMOITEMS_NAME")) {
      name = stackNbt.getString("MMOITEMS_NAME");

      // Remove any formatting codes from the name
      // - <tier-name>
      // - <tier-color>
      // - <tier-color-cleaned>
      name = name.replaceAll("<[^>]+>", "");

      // Also remove any color codes from the name
      name = name.replaceAll("[§&][0-9a-f]", "");
    }

    return name;
  }

  public static Optional<Integer> getRevisionId(ItemStack itemStack) {
    // Try to parse "MMOITEMS_REVISION_ID" tag from the item stack
    NbtCompound stackNbt = itemStack.getNbt();
    if (stackNbt != null && stackNbt.contains("MMOITEMS_REVISION_ID")) {
      return Optional.of(stackNbt.getInt("MMOITEMS_REVISION_ID"));
    }

    return Optional.empty();
  }

  public static boolean isItemEqual(ItemStack a, ItemStack b) {
    String aKey = ItemUtil.getKey(a);
    String bKey = ItemUtil.getKey(b);

    return aKey.equals(bKey);
  }

  public static @Nullable RecipeEntry<?> getRecipe(Identifier id) {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client.world != null && id != null) {
      RecipeManager manager = client.world.getRecipeManager();
      if (manager != null) {
        return manager.get(id).orElse(null);
      }
    }

    return null;
  }

  public static ItemStack getOutput(Recipe<?> recipe) {
    MinecraftClient client = MinecraftClient.getInstance();
    return recipe.getResult(client.world.getRegistryManager());
  }

  /**
   * @return True if the given item key should be considered fully decomposed. These items may have a known
   *  recipe, but it shouldn't be used during decomposition.
   */
  public static boolean isFullyDecomposed(String key) {
    return BASE_ITEMS.contains(key);
  }

  public static boolean isRecursiveRecipe(JournalEntry entry, String key) {
    if (entry == null || Journal.INSTANCE == null) {
      return false;
    }

    for (String ingredientKey : entry.getIngredients().keySet()) {
      if (ingredientKey.equals(key)) {
        return true;
      }
    }

    return false;
  }

  public static boolean isRecursiveRecipe(RecipeEntry<?> recipeEntry, String key) {
    if (recipeEntry == null || !(recipeEntry.value() instanceof CraftingRecipe recipe)) {
      return false;
    }

    for (Ingredient ingredient : recipe.getIngredients()) {
      ItemStack[] stacks = ingredient.getMatchingStacks();
      for (ItemStack stack : stacks) {
        if (ItemUtil.getKey(stack).equals(key)) {
          return true;
        }
      }
    }

    return false;
  }
}
