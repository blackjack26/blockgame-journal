package dev.bnjc.blockgamejournal.journal;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Map;

public record JournalMode(JournalMode.Type type, Item icon, int order) {
  public static final Map<Type, JournalMode> MODES = Map.of(
      Type.FAVORITES, new JournalMode(Type.FAVORITES, Items.GLISTERING_MELON_SLICE, 0),
      Type.ITEM_SEARCH, new JournalMode(Type.ITEM_SEARCH, Items.COMPASS, 1),
      Type.NPC_SEARCH, new JournalMode(Type.NPC_SEARCH, Items.PLAYER_HEAD, 2),
      Type.INGREDIENT_SEARCH, new JournalMode(Type.INGREDIENT_SEARCH, Items.DRAGON_BREATH, 3)
  );

  public enum Type {
    ITEM_SEARCH,
    NPC_SEARCH,
    FAVORITES,
    INGREDIENT_SEARCH;

    @Override
    public String toString() {
      return I18n.translate("blockgamejournal.menu.mode." + this.name());
    }
  }
}
