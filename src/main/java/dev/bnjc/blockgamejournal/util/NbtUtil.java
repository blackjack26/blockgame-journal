package dev.bnjc.blockgamejournal.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.Nullable;

public class NbtUtil {
  public static @Nullable NbtList getLore(ItemStack stack) {
    NbtCompound tag = stack.getNbt();
    if (tag == null) {
      return null;
    }

    if (!tag.contains(ItemStack.DISPLAY_KEY, NbtElement.COMPOUND_TYPE)) {
      return null;
    }

    NbtCompound display = tag.getCompound(ItemStack.DISPLAY_KEY);
    if (!display.contains(ItemStack.LORE_KEY, NbtElement.LIST_TYPE)) {
      return null;
    }

    return display.getList(ItemStack.LORE_KEY, NbtElement.STRING_TYPE);
  }
}
