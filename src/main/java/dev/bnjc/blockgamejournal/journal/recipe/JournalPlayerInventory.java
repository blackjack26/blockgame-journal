package dev.bnjc.blockgamejournal.journal.recipe;

import dev.bnjc.blockgamejournal.util.ItemUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JournalPlayerInventory {
  private final Map<String, ItemStack> inventory = new HashMap<>();

  public JournalPlayerInventory(List<ItemStack> stacks) {
    stacks.forEach(this::addStack);
  }

  private JournalPlayerInventory(@NotNull PlayerEntity entity) {
    entity.getInventory().main.forEach(this::addStack);
    entity.getInventory().armor.forEach(this::addStack);
    entity.getInventory().offHand.forEach(this::addStack);
  }

  public static JournalPlayerInventory of(@Nullable PlayerEntity entity) {
    if (entity == null) {
      return new JournalPlayerInventory(List.of());
    }
    return new JournalPlayerInventory(entity);
  }

  public static JournalPlayerInventory defaultInventory() {
    return JournalPlayerInventory.of(MinecraftClient.getInstance().player);
  }

  public boolean hasEnough(ItemStack stack) {
    return this.hasEnough(stack, stack.getCount());
  }

  public boolean hasEnough(ItemStack stack, int count) {
    String key = ItemUtil.getKey(stack);
    ItemStack inventoryStack = this.inventory.get(key);
    if (inventoryStack == null) {
      return false;
    }

    return inventoryStack.getCount() >= count;
  }

  private void addStack(ItemStack stack) {
    if (stack.isEmpty()) {
      return;
    }

    String key = ItemUtil.getKey(stack);
    this.inventory.merge(key, stack, (a, b) -> {
      a.setCount(a.getCount() + b.getCount());
      return a;
    });
  }
}
