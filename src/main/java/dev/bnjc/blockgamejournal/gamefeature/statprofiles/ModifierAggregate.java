package dev.bnjc.blockgamejournal.gamefeature.statprofiles;

import lombok.Getter;
import net.minecraft.util.Formatting;

@Getter
public class ModifierAggregate {
  private final String name;
  private final ModifierType type;
  private final ModifierCategory category;
  private final int order;
  private final String icon;

  private double value;

  public ModifierAggregate(String name, ModifierType type, ModifierCategory category) {
    this.name = name;
    this.type = type;
    this.category = category;
    this.order = ModifierAggregate.getOrder(name);
    this.icon = ModifierAggregate.getIcon(name);

    this.value = 0;
  }

  public void addValue(double value) {
    this.value += value;
  }

  public String getDisplayName() {
    return switch (name) {
      case "Critical Strike Chance" -> "Critical Chance";
      case "Critical Strike Power" -> "Critical Power";
      case "Health Regeneration" -> "Health Regen";
      default -> name;
    };
  }

  public Formatting getValueColor() {
    if (value > 0) {
      return Formatting.GREEN;
    } else if (value < 0) {
      return Formatting.RED;
    } else {
      return Formatting.WHITE;
    }
  }

  private static String getIcon(String name) {
    return switch (name) {
      case "Critical Strike Chance", "Critical Strike Power", "All Damage", "Weapon Damage", "Backstab Damage",
           "PVE Damage", "PVP Damage" -> "\uD83D\uDDE1";
      case "Magic Damage" -> "☄";
      case "Projectile Damage" -> "\uD83C\uDFF9";
      case "Thaumaturgy Power" -> "☮";
      case "AOE Size Amplifier" -> "◎";
      case "Cooldown Reduction" -> "⏳";
      case "Movement Speed" -> "⌚";
      case "Max Health", "Health Regeneration" -> "❤";
      case "Defense", "Block Cooldown Reduction", "Block Power", "Block Rating", "Knockback Resistance",
           "Damage Reduction", "PVE Damage Reduction", "PVP Damage Reduction" -> "⛨";
      default -> "";
    };
  }

  private static int getOrder(String name) {
    return switch (name) {
      case "Critical Strike Chance" -> 0;
      case "Critical Strike Power" -> 1;
      case "All Damage" -> 2;
      case "Weapon Damage" -> 3;
      case "Magic Damage" -> 4;
      case "Projectile Damage" -> 5;
      case "Thaumaturgy Power" -> 6;
      case "Backstab Damage" -> 7;
      case "PVE Damage" -> 8;
      case "PVP Damage" -> 9;
      case "AOE Size Amplifier" -> 10;
      case "Cooldown Reduction" -> 11;
      case "Movement Speed" -> 12;
      case "Max Health" -> 13;
      case "Health Regeneration" -> 14;
      case "Defense" -> 15;
      case "Block Rating" -> 16;
      case "Block Power" -> 17;
      case "Block Cooldown Reduction" -> 18;
      case "Knockback Resistance" -> 19;
      case "Damage Reduction" -> 20;
      case "PVE Damage Reduction" -> 21;
      case "PVP Damage Reduction" -> 22;
      default -> -1;
    };
  }
}
