package dev.bnjc.blockgamejournal.util;

import dev.bnjc.blockgamejournal.journal.JournalEntryBuilder;
import net.minecraft.item.ItemStack;
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
    NbtList lore = NbtUtil.getLore(stack);
    if (lore == null) {
      return false;
    }

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
