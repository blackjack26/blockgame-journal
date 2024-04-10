package dev.bnjc.blockgamejournal.util;

import dev.bnjc.blockgamejournal.journal.JournalEntryBuilder;
import net.minecraft.item.ItemStack;

public class ItemUtil {
  public static boolean isItemEqual(ItemStack a, ItemStack b) {
    String aKey = JournalEntryBuilder.getKey(a);
    String bKey = JournalEntryBuilder.getKey(b);

    return aKey.equals(bKey);
  }
}
