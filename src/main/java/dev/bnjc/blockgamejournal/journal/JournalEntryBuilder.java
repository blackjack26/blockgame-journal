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
  private final int slot;

  public JournalEntryBuilder(List<ItemStack> ingredientStacks, PlayerEntity npc, int slot) {
    this.ingredientStacks = ingredientStacks;
    this.npc = npc;
    this.slot = slot;
  }

  public JournalEntry build(ItemStack stack) {
    return new JournalEntry(stack, getIngredients(), npc.getEntityName(), slot, System.currentTimeMillis());
  }

  private Map<String, Integer> getIngredients() {
    Map<String, Integer> ingredients = new HashMap<>();
    for (ItemStack stack : ingredientStacks) {
      ingredients.compute(ItemUtil.getKey(stack), (key, value) -> value == null ? stack.getCount() : value + stack.getCount());
    }
    return ingredients;
  }
}
