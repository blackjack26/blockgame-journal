package dev.bnjc.blockgamejournal.util;

import dev.bnjc.blockgamejournal.journal.JournalEntryBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.apache.commons.lang3.StringUtils;

public class SearchUtil {
  public static boolean defaultPredicate(ItemStack stack, String filter) {
    return namePredicate(stack, filter) || lorePredicate(stack, filter);
  }

  public static boolean namePredicate(ItemStack stack, String filter) {
    return StringUtils.containsIgnoreCase(JournalEntryBuilder.getName(stack), filter);
  }

  public static boolean lorePredicate(ItemStack stack, String filter) {
    NbtCompound tag = stack.getNbt();
    if (tag == null) {
      return false;
    }

    if (!tag.contains(ItemStack.DISPLAY_KEY, NbtElement.COMPOUND_TYPE)) {
      return false;
    }

    NbtCompound display = tag.getCompound(ItemStack.DISPLAY_KEY);
    if (!display.contains(ItemStack.LORE_KEY, NbtElement.LIST_TYPE)) {
      return false;
    }

    NbtList lore = display.getList(ItemStack.LORE_KEY, NbtElement.STRING_TYPE);
    for (int i = 0; i < lore.size(); i++) {
      String line = lore.getString(i);
      try {
        if (StringUtils.containsIgnoreCase(line, filter)) {
          return true;
        }
      } catch (Exception e) {
        return false;
      }
    }

    return false;
  }
}
