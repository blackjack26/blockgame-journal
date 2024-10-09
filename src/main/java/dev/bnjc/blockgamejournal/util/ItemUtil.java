package dev.bnjc.blockgamejournal.util;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.station.CraftingStationItem;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

  public static @Nullable List<Text> checkOutdated(JournalEntry currentEntry, CraftingStationItem newStationItem) {
    if (newStationItem.getOutdated() != null) {
      return newStationItem.getOutdated();
    }

    List<Text> outdatedReasons = new ArrayList<>();

    // Check if cost changed
    if (newStationItem.getCost() != currentEntry.getCost()) {
      outdatedReasons.add(
          Text.literal("[△] ")
              .formatted(Formatting.AQUA)
              .append(Text.literal(currentEntry.getCost() + " coin → " + newStationItem.getCost() + " coin")
                  .formatted(Formatting.GRAY))
      );
    }

    // TODO: Same amount of ingredients, but different ingredients
    List<String> currentIngredientNames = new ArrayList<>();
    for (String key : currentEntry.getIngredients().keySet()) {
      ItemStack stack = Journal.INSTANCE.getKnownItem(key);
      if (stack == null) {
        BlockgameJournal.LOGGER.warn("[Blockgame Journal] Ingredient not known: {} - {}", key, newStationItem.getItem());
        continue;
      }

      // The ingredient is not in the entry
      String itemName = ItemUtil.getName(stack, true);
      currentIngredientNames.add(itemName);
      if (!newStationItem.getExpectedIngredients().containsKey(itemName)) {
        outdatedReasons.add(
            Text.literal("[-] ")
                .formatted(Formatting.RED)
                .append(Text.literal(currentEntry.getIngredients().get(key) + " " + itemName)
                    .formatted(Formatting.GRAY))
        );
        continue;
      }

      // The ingredient amount is different
      int expectedAmount = newStationItem.getExpectedIngredients().get(itemName);
      int currentAmount = currentEntry.getIngredients().get(key);
      if (expectedAmount != currentAmount) {
        outdatedReasons.add(
            Text.literal("[△] ")
                .formatted(Formatting.AQUA)
                .append(Text.literal(currentAmount + " " + itemName + " → " + expectedAmount + " " + itemName)
                    .formatted(Formatting.GRAY))
        );
      }
    }

    for (var entry : newStationItem.getExpectedIngredients().entrySet()) {
      if (!currentIngredientNames.contains(entry.getKey())) {
        outdatedReasons.add(
            Text.literal("[+] ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(entry.getValue() + " " + entry.getKey())
                    .formatted(Formatting.GRAY))
        );
      }
    }

    // Check to see if there is a new class requirement
    if (!Objects.equals(currentEntry.getRequiredClass(), newStationItem.getRequiredClass())) {
      if (newStationItem.getRequiredClass().isEmpty()) {
        outdatedReasons.add(
            Text.literal("[-] ")
                .formatted(Formatting.RED)
                .append(Text.literal(currentEntry.getRequiredLevel() + " in " + currentEntry.getRequiredClass())
                    .formatted(Formatting.GRAY))
        );
      } else if (currentEntry.getRequiredClass().isEmpty()) {
        outdatedReasons.add(
            Text.literal("[+] ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(newStationItem.getRequiredLevel() + " in " + newStationItem.getRequiredClass())
                    .formatted(Formatting.GRAY))
        );
      } else {
        outdatedReasons.add(
            Text.literal("[△] ")
                .formatted(Formatting.AQUA)
                .append(Text.literal(currentEntry.getRequiredLevel() + " in " + currentEntry.getRequiredClass() + " → " + newStationItem.getRequiredLevel() + " in " + newStationItem.getRequiredClass())
                    .formatted(Formatting.GRAY))
        );
      }
    }
    // Check to see if there is a new level requirement
    else if (currentEntry.getRequiredLevel() != newStationItem.getRequiredLevel()) {
      outdatedReasons.add(
          Text.literal("[△] ")
              .formatted(Formatting.AQUA)
              .append(Text.literal(currentEntry.getRequiredLevel() + " in " + currentEntry.getRequiredClass() + " → " + newStationItem.getRequiredLevel() + " in " + newStationItem.getRequiredClass())
                  .formatted(Formatting.GRAY))
      );
    }

    // Check to see if recipe has been added
    if (currentEntry.recipeKnown() == null && newStationItem.getRecipeKnown() != null) {
      outdatedReasons.add(
          Text.literal("[+] ")
              .formatted(Formatting.GREEN)
              .append(Text.literal("Recipe requirement")
                  .formatted(Formatting.GRAY))
      );
    }

    // Check to see if recipe has been removed
    if (currentEntry.recipeKnown() != null && newStationItem.getRecipeKnown() == null) {
      outdatedReasons.add(
          Text.literal("[-] ")
              .formatted(Formatting.RED)
              .append(Text.literal("Recipe requirement")
                  .formatted(Formatting.GRAY))
      );
    }

    // Item revision ID is different
    Optional<Integer> slotRevId = ItemUtil.getRevisionId(newStationItem.getItem());
    if (slotRevId.isPresent() && slotRevId.get() != currentEntry.getRevisionId()) {
      outdatedReasons.add(
          Text.literal("[△] ")
              .formatted(Formatting.AQUA)
              .append(Text.literal("Revision ID " + currentEntry.getRevisionId() + " → " + slotRevId.get())
                  .formatted(Formatting.GRAY))
      );
    }

    if (outdatedReasons.isEmpty()) {
      return null;
    }

    return outdatedReasons;
  }

  public static void renderItemCount(DrawContext context, int x, int y, int count) {
    if (count <= 1) {
      return;
    }

    context.getMatrices().push();
    context.getMatrices().translate(0.0f, 0.0f, 200.0f);

    Text text = Text.literal("" + count).formatted(Formatting.WHITE);
    context.drawText(MinecraftClient.getInstance().textRenderer, text, x + 19 - 2 - MinecraftClient.getInstance().textRenderer.getWidth(text), y + 6 + 3, 0xFFFFFF, true);
    context.getMatrices().pop();
  }
}
