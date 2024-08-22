package dev.bnjc.blockgamejournal.util;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.station.CraftingStationItem;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
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
      "mmoitems:ESSENCE_EARTH",
      "mmoitems:PRISTINE_STONE",
      "mmoitems:PRISTINE_WOOD",
      "mmoitems:PRISTINE_HIDE",
      "mmoitems:SALT",
      "mmoitems:PEPPER"
  );

  /**
   * These are items that have a different name in the tooltip than in the item name. There will probably be more
   * that will be discovered over time (but hopefully not too many).
   */
  private static final Map<String, String> ITEM_TO_TOOLTIP = Map.of(
      "Rabbit's Foot", "Rabbit Foot",
      "Jack o'Lantern", "Jack O Lantern",
      "Lily of the Valley", "Lily Of The Valley",
      "Redstone Dust", "Redstone",
      "Block of Amethyst", "Amethyst Block",
      "Slimeball", "Slime Ball"
  );

  public static String getKey(ItemStack itemStack) {
    String key = Registries.ITEM.getId(itemStack.getItem()).toString();

    // If result has "MMOITEMS_ITEM_ID" tag, then it is an MMOItems item. Otherwise, it is a vanilla item.
    NbtCompound stackNbt = itemStack.getNbt();
    if (stackNbt != null && stackNbt.contains("MMOITEMS_ITEM_ID")) {
      key = "mmoitems:" + stackNbt.getString("MMOITEMS_ITEM_ID");

      if (stackNbt.contains("MMOITEMS_NAME_PRE")) {
        String namePre = stackNbt.getString("MMOITEMS_NAME_PRE");
        if (namePre.contains("Corrupted")) {
          key += "_CORRUPTED";
        }
      }
    }

    return key;
  }

  public static String getName(ItemStack itemStack, boolean mapToTooltip) {
    String name = ItemUtil.getName(itemStack);

    if (mapToTooltip) {
      name = ITEM_TO_TOOLTIP.getOrDefault(name, name);
    }

    return name;
  }

  public static String getName(ItemStack itemStack) {
    String name = itemStack.getName().getString();

    // If result has "MMOITEMS_NAME" tag, then it is an MMOItems item. Otherwise, it is a vanilla item.
    NbtCompound stackNbt = itemStack.getNbt();
    if (stackNbt != null) {
      if (stackNbt.contains("MMOITEMS_NAME")) {
        name = stackNbt.getString("MMOITEMS_NAME");

        // Remove any formatting codes from the name
        // - <tier-name>
        // - <tier-color>
        // - <tier-color-cleaned>
        name = name.replaceAll("<[^>]+>", "");

        // Also remove any color codes from the name
        name = name.replaceAll("[§&][0-9a-f]", "");
      }

      if (stackNbt.contains("MMOITEMS_NAME_PRE")) {
        String namePre = stackNbt.getString("MMOITEMS_NAME_PRE");
        if (namePre.contains("Corrupted")) {
          name = "§d§lCorrupted§r " + name;
        }
      }
    }

    return name;
  }

  public static ItemStack getGoldItem(int count) {
    ItemStack item = null;

    if (Journal.INSTANCE != null) {
      item = Journal.INSTANCE.getKnownItem("mmoitems:GOLD_COIN");
    }

    if (item == null) {
      item = new ItemStack(Items.GOLD_NUGGET, count);
      NbtCompound stackNbt = item.getOrCreateNbt();
      stackNbt.putString("MMOITEMS_ITEM_ID", "GOLD_COIN");
      stackNbt.putString("MMOITEMS_NAME", "Gold Coin");
      item.setNbt(stackNbt);
    }

    return item;
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

  public static boolean isOutdated(JournalEntry entry, CraftingStationItem expected) {
    if (expected.getOutdated() != null) {
      return expected.getOutdated();
    }

    // Item revision ID is different
    Optional<Integer> slotRevId = ItemUtil.getRevisionId(expected.getItem());
    if (slotRevId.isPresent() && slotRevId.get() != entry.getRevisionId()) {
      return true;
    }

    // Different amount of ingredients
    if (entry.getIngredients().size() != expected.getExpectedIngredients().size()) {
      return true;
    }

    // TODO: Same amount of ingredients, but different ingredients
    for (String key : entry.getIngredients().keySet()) {
      ItemStack stack = Journal.INSTANCE.getKnownItem(key);
      if (stack == null) {
        BlockgameJournal.LOGGER.warn("[Blockgame Journal] Ingredient not known: {} - {}", key, expected.getItem());
        break;
      }

      // The ingredient is not in the entry
      String itemName = ItemUtil.getName(stack, true);
      if (!expected.getExpectedIngredients().containsKey(itemName)) {
        BlockgameJournal.LOGGER.warn("[Blockgame Journal] Ingredient not expected: {} - {}", itemName, expected.getItem());
        return true;
      }

      // The ingredient amount is different
      int expectedAmount = expected.getExpectedIngredients().get(itemName);
      int actualAmount = entry.getIngredients().get(key);
      if (expectedAmount != actualAmount) {
        BlockgameJournal.LOGGER.warn("[Blockgame Journal] Ingredient amount mismatch: {} (expected: {}, actual: {}) - {}", key, expectedAmount, actualAmount, expected.getItem());
        return true;
      }
    }

    return false;
  }
}
