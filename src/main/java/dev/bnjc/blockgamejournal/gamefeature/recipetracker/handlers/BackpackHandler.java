package dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.RecipeTrackerGameFeature;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.listener.interaction.SlotClickedListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenOpenedListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenReceivedInventoryListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenSlotUpdateListener;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackpackHandler {
  private static final Logger LOGGER = BlockgameJournal.getLogger("Backpack");

  private final RecipeTrackerGameFeature gameFeature;
  private final List<ItemStack> inventory = new ArrayList<>();

  private int syncId = -1;
  private int lastClickedSlot = -1;

  public BackpackHandler(RecipeTrackerGameFeature gameFeature) {
    this.gameFeature = gameFeature;
  }

  public void init() {
    ScreenOpenedListener.EVENT.register(this::handleOpenScreen);
    ScreenReceivedInventoryListener.EVENT.register(this::handleReceivedInventory);
    ScreenSlotUpdateListener.EVENT.register(this::handleSlotUpdate);
    SlotClickedListener.EVENT.register(this::handleSlotClick);
  }

  private ActionResult handleSlotClick(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player) {
    lastClickedSlot = -1;

    if (syncId != this.syncId) {
      return ActionResult.PASS;
    }

    if (button == 0 && actionType == SlotActionType.QUICK_MOVE) {
      lastClickedSlot = slotId;
    }

    return ActionResult.PASS;
  }

  private ActionResult handleSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet) {
    if (syncId == -1 || packet.getSyncId() != syncId || lastClickedSlot == -1) {
      return ActionResult.PASS;
    }

    int slot = packet.getSlot();
    ItemStack itemStack = packet.getStack();

    // Shift-clicked a non-backpack slot, add the item to the backpack
    if (slot < inventory.size() && lastClickedSlot >= inventory.size()) {
      inventory.set(slot, itemStack);
      this.updateBackpackMetadata();
    }
    // Shift-clicked a backpack slot, remove the item from the backpack
    else if (slot >= inventory.size() && lastClickedSlot < inventory.size()) {
      inventory.set(lastClickedSlot, ItemStack.EMPTY);
      this.updateBackpackMetadata();
    }

    return ActionResult.PASS;
  }

  private ActionResult handleOpenScreen(OpenScreenS2CPacket packet) {
    String screenName = packet.getName().getString().replaceAll("[§&][0-9a-f]", "");

    // Look for screen name "Backpack"
    if (screenName.equals("Backpack")) {
      syncId = packet.getSyncId();
    } else {
      syncId = -1;
    }

    inventory.clear();
    lastClickedSlot = -1;

    return ActionResult.PASS;
  }

  private ActionResult handleReceivedInventory(InventoryS2CPacket packet) {
    if (packet.getSyncId() != syncId) {
      return ActionResult.PASS;
    }

    // Ignore the last 4 rows of the inventory (these are not backpack slots)
    List<ItemStack> contents = packet.getContents();
    inventory.clear();
    inventory.addAll(packet.getContents().subList(0, contents.size() - 9 * 4));

    this.updateBackpackMetadata();

    return ActionResult.PASS;
  }

  private void updateBackpackMetadata() {
    Map<String, Integer> backpackContents = new HashMap<>();
    for (ItemStack itemStack : inventory) {
      if (itemStack == null || itemStack.isEmpty()) {
        continue;
      }

      String itemKey = ItemUtil.getKey(itemStack);
      int count = itemStack.getCount();
      backpackContents.compute(itemKey, (k, v) -> (v == null) ? count : v + count);
    }

    Journal.instance().getMetadata().setBackpack(backpackContents);
  }
}
