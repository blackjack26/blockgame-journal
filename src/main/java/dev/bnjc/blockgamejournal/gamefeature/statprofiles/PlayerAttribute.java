package dev.bnjc.blockgamejournal.gamefeature.statprofiles;

import dev.bnjc.blockgamejournal.gui.screen.StatScreen;
import dev.bnjc.blockgamejournal.util.NbtUtil;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Getter
public class PlayerAttribute {
  private static final Pattern POINTS_PATTERN = Pattern.compile("Points Spent: (\\d+)/(\\d+)");
  private static final Pattern COST_PATTERN = Pattern.compile("Click to level up for (\\d+) attribute point.*");
  private static final Pattern CURRENT_POINTS_PATTERN = Pattern.compile(".*Current Attribute Points: (\\d+)");

  private final String name;
  private final int max;
  private final int spent;
  private final Set<StatModifier> buffs;
  private final int cost;
  private final int slot;
  private final ItemStack itemStack;
  private final AttributeCategory category;

  public PlayerAttribute(String name, int max, int spent, Set<StatModifier> buffs, int cost, int slot, ItemStack itemStack) {
    this.name = name;
    this.max = max;
    this.spent = spent;
    this.buffs = buffs;
    this.cost = cost;
    this.slot = slot;
    this.itemStack = itemStack;
    this.category = determineCategory(name);
  }

  public static @Nullable PlayerAttribute fromItem(ItemStack stack, int slot) {
    if (stack == null || stack.getItem() == Items.AIR) {
      return null;
    }

    NbtList loreList = NbtUtil.getLore(stack);
    if (loreList == null) {
      return null;
    }

    String name = stack.getName().getString();
    int max = -1;
    int spent = -1;
    int cost = -1;
    Set<StatModifier> buffs = new HashSet<>();

    boolean inBuffs = false;
    for (int i = 0; i < loreList.size(); i++) {
      MutableText loreText = NbtUtil.parseLore(loreList, i);
      if (loreText == null) {
        continue;
      }

      String loreString = loreText.getString();
      if (loreString.isEmpty()) {
        inBuffs = false;
        continue;
      }

      // Line matching "Points Spent: 1/10" is the max and spent values
      var matcher = POINTS_PATTERN.matcher(loreString);
      if (matcher.matches()) {
        spent = Integer.parseInt(matcher.group(1));
        max = Integer.parseInt(matcher.group(2));
        StatScreen.LOGGER.debug(" Found spent: {}, max: {}", spent, max);
        continue;
      }

      // Buffs start with a "When Leveled Up:" line
      if (loreString.startsWith("When Leveled Up:")) {
        inBuffs = true;
        continue;
      }

      // Buffs are in the format of "  +1.0% Critical Strike Chance (+1%)"
      if (inBuffs) {
        StatModifier buff = StatModifier.fromLore(loreString);
        if (buff != null) {
          buffs.add(buff);
          StatScreen.LOGGER.debug(" Found buff: {}", buff);
          continue;
        }
      }

      // Get cost from "Click to level up for 1 attribute point."
      matcher = COST_PATTERN.matcher(loreString);
      if (matcher.matches()) {
        cost = Integer.parseInt(matcher.group(1));
        StatScreen.LOGGER.debug(" Found cost: {}", cost);
        continue;
      }
    }

    return new PlayerAttribute(name, max, spent, buffs, cost, slot, stack);
  }

  public static int getAvailablePoints(ItemStack stack) {
    NbtList loreList = NbtUtil.getLore(stack);
    if (loreList == null) {
      return -1;
    }

    // Start from the end of the lore list to find the most recent "Current Attribute Points: X" line
    for (int i = loreList.size() - 1; i >= 0; i--) {
      MutableText loreText = NbtUtil.parseLore(loreList, i);
      if (loreText == null) {
        continue;
      }

      String loreString = loreText.getString();
      var matcher = CURRENT_POINTS_PATTERN.matcher(loreString);
      if (matcher.matches()) {
        return Integer.parseInt(matcher.group(1));
      }
    }

    return -1;
  }

  private AttributeCategory determineCategory(String name) {
    return switch (name) {
      case "Ferocity", "Spartan", "Dexterity", "Alacrity", "Intelligence", "Precision", "Assassin", "Glass Cannon",
           "Bravery", "Bullying", "Battleship", "Dreadnought" -> AttributeCategory.OFFENSE;
      case "Wisdom", "Pacifist", "Volition", "Somatics", "Bloom", "Time Lord" -> AttributeCategory.SUPPORT;
      case "Tenacity", "Shield Mastery", "Fortress", "Juggernaut", "Beef Cake", "Chunky Soup" -> AttributeCategory.DEFENSE;
      default -> AttributeCategory.OTHER;
    };
  }
}
