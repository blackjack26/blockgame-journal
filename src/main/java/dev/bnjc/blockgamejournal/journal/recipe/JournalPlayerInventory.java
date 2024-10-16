package dev.bnjc.blockgamejournal.journal.recipe;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.journal.Journal;
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
  private final Map<String, ItemStack> editableInventory = new HashMap<>();

  public JournalPlayerInventory(List<ItemStack> stacks) {
    stacks.forEach(this::addStack);
    this.resetEditable();
  }

  private JournalPlayerInventory(@NotNull PlayerEntity entity) {
    entity.getInventory().main.forEach(this::addStack);
    entity.getInventory().armor.forEach(this::addStack);
    entity.getInventory().offHand.forEach(this::addStack);

    if (BlockgameJournal.getConfig().getDecompositionConfig().useBackpackItems) {
      Journal.INSTANCE.getMetadata().getBackpackContents().forEach(this::addStack);
    }

    this.resetEditable();
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

  public int neededCount(ItemStack stack) {
    return this.neededCount(stack, stack.getCount());
  }

  public int neededCount(ItemStack stack, int count) {
    String key = ItemUtil.getKey(stack);
    ItemStack inventoryStack = this.inventory.get(key);
    if (inventoryStack == null) {
      return count;
    }

    return count - inventoryStack.getCount();
  }

  public int count(ItemStack stack) {
    String key = ItemUtil.getKey(stack);
    ItemStack inventoryStack = this.inventory.get(key);
    if (inventoryStack == null) {
      return 0;
    }

    return inventoryStack.getCount();
  }

  public int consume(ItemStack stack, int count) {
    String key = ItemUtil.getKey(stack);
    ItemStack inventoryStack = this.editableInventory.get(key);
    if (inventoryStack == null) {
      return count;
    }

    int leftover = count - inventoryStack.getCount();
    inventoryStack.setCount(Math.max(inventoryStack.getCount() - count, 0));

    return leftover;
  }

  public void resetEditable() {
    this.editableInventory.clear();
    this.inventory.forEach((key, stack) -> this.editableInventory.put(key, stack.copy()));
  }

  private void addStack(ItemStack stack) {
    if (stack.isEmpty()) {
      return;
    }

    String key = ItemUtil.getKey(stack);
    this.inventory.merge(key, stack.copy(), (a, b) -> {
      a.setCount(a.getCount() + b.getCount());
      return a;
    });
  }

  private void addStack(String key, int count) {
    ItemStack stack = Journal.INSTANCE.getKnownItem(key);
    if (stack == null) {
      return;
    }

    stack.setCount(count);
    this.addStack(stack);
  }
}
