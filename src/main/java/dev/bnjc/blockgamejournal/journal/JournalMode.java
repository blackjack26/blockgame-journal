package dev.bnjc.blockgamejournal.journal;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Map;

public record JournalMode(JournalMode.Type type, Item icon, int order) {
  public static final Map<Type, JournalMode> MODES = Map.of(
      JournalMode.Type.ITEM_SEARCH, new JournalMode(JournalMode.Type.ITEM_SEARCH, Items.GLISTERING_MELON_SLICE, 0),
      JournalMode.Type.NPC_SEARCH, new JournalMode(JournalMode.Type.NPC_SEARCH, Items.PLAYER_HEAD, 1),
      JournalMode.Type.FAVORITES, new JournalMode(JournalMode.Type.FAVORITES, Items.EMERALD, 2)
  );

  public enum Type {
    ITEM_SEARCH,
    NPC_SEARCH,
    FAVORITES,
  }
}
