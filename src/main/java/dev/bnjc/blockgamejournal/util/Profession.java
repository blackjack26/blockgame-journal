package dev.bnjc.blockgamejournal.util;

import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.jetbrains.annotations.Nullable;

@Getter
public enum Profession {
  MINING("Mining", 10, Items.IRON_PICKAXE),
  LOGGING("Logging", 11, Items.IRON_AXE),
  ARCHAEOLOGY("Archaeology", 12, Items.IRON_SHOVEL),
  EINHERJAR("Profile", 15, Items.PLAYER_HEAD),
  FISHING("Fishing", 19, Items.FISHING_ROD),
  HERBALISM("Herbalism", 20, Items.IRON_HOE),
  RUNECARVING("Runecarving", 21, Items.TURTLE_EGG),
  COOKING("Cooking", 28, Items.CAMPFIRE);

  private final String name;
  private final int slot;
  private final Item item;

  Profession(String name, int slot, Item item) {
    this.name = name;
    this.slot = slot;
    this.item = item;
  }

  public static @Nullable Profession fromClass(String className) {
    for (Profession profession : values()) {
      if (profession.name().equalsIgnoreCase(className)) {
        return profession;
      }
    }
    return null;
  }
}
