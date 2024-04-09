package dev.bnjc.blockgamejournal.util;

import lombok.Getter;
import net.minecraft.util.Formatting;

@Getter
public enum Tier {
  COMMON(Formatting.WHITE),
  UNCOMMON(Formatting.GREEN),
  RARE(Formatting.AQUA),
  EPIC(Formatting.DARK_PURPLE),
  QUEST(Formatting.GOLD);

  private final Formatting color;

  Tier(Formatting color) {
    this.color = color;
  }

  public static Tier fromString(String tier) {
    if (tier.startsWith("COMMON")) {
      return COMMON;
    } else if (tier.startsWith("UNCOMMON")) {
      return UNCOMMON;
    } else if (tier.startsWith("RARE")) {
      return RARE;
    } else if (tier.startsWith("EPIC")) {
      return EPIC;
    } else if (tier.startsWith("QUEST")) {
      return QUEST;
    } else {
      return COMMON;
    }
  }
}
