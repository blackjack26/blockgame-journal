package dev.bnjc.blockgamejournal.journal;

import lombok.Getter;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@Getter
public class JournalItemStack {
  private final ItemStack stack;
  private final int slot;

  public JournalItemStack(ItemStack itemStack) {
    this(itemStack, -1);
  }

  public JournalItemStack(ItemStack stack, int slot) {
    this.stack = stack;
    this.slot = slot;
  }

  public static @Nullable JournalItemStack fromKnownItem(String key) {
    if (Journal.INSTANCE == null) {
      return null;
    }

    ItemStack stack = Journal.INSTANCE.getKnownItem(key);
    if (stack == null) {
      return null;
    }

    return new JournalItemStack(stack);
  }
}
