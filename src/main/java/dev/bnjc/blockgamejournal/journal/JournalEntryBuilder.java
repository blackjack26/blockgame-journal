package dev.bnjc.blockgamejournal.journal;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JournalEntryBuilder {
  private final List<ItemStack> ingredientStacks;
  private final PlayerEntity npc;

  public JournalEntryBuilder(List<ItemStack> ingredientStacks, PlayerEntity npc) {
    this.ingredientStacks = ingredientStacks;
    this.npc = npc;
  }

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
    }

    return name;
  }

  public JournalEntry build() {
    return new JournalEntry(getIngredients(), npc.getId(), npc.getEntityName(), System.currentTimeMillis());
  }

  private Map<String, Integer> getIngredients() {
    Map<String, Integer> ingredients = new HashMap<>();
    for (ItemStack stack : ingredientStacks) {
      ingredients.compute(getKey(stack), (key, value) -> value == null ? stack.getCount() : value + stack.getCount());
    }
    return ingredients;
  }
}
