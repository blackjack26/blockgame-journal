package dev.bnjc.blockgamejournal.journal;

import dev.bnjc.blockgamejournal.util.ItemUtil;
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

  public JournalEntry build(ItemStack stack) {
    return new JournalEntry(ItemUtil.getKey(stack), stack.getCount(), getIngredients(), npc.getEntityName(), System.currentTimeMillis());
  }

  private Map<String, Integer> getIngredients() {
    Map<String, Integer> ingredients = new HashMap<>();
    for (ItemStack stack : ingredientStacks) {
      ingredients.compute(ItemUtil.getKey(stack), (key, value) -> value == null ? stack.getCount() : value + stack.getCount());
    }
    return ingredients;
  }
}
