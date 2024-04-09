package dev.bnjc.blockgamejournal.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public final class JournalEntry {
  public static final Codec<JournalEntry> CODEC = RecordCodecBuilder.create(instance ->
          instance.group(
              Codec.STRING.fieldOf("key").forGetter(JournalEntry::getKey),
              Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("ingredients").forGetter(JournalEntry::getIngredients),
              Codec.STRING.fieldOf("npcName").forGetter(JournalEntry::getNpcName),
              Codec.LONG.fieldOf("storedAt").forGetter(JournalEntry::getStoredAt),
              Codec.BYTE.orElse((byte) -1).fieldOf("recipeKnown").forGetter(JournalEntry::getRecipeKnown),
              Codec.FLOAT.orElse(-1.0f).fieldOf("cost").forGetter(JournalEntry::getCost),
              Codec.STRING.orElse("").fieldOf("requiredClass").forGetter(JournalEntry::getRequiredClass),
              Codec.INT.orElse(-1).fieldOf("requiredLevel").forGetter(JournalEntry::getRequiredLevel)
          ).apply(instance, (key, ingredients, npcName, storedAt, recipeKnown, cost, requiredClass, requiredLevel) -> {
            JournalEntry entry = new JournalEntry(key, ingredients,  npcName, storedAt);
            entry.setRecipeKnown(recipeKnown);
            entry.setCost(cost);
            entry.setRequiredClass(requiredClass);
            entry.setRequiredLevel(requiredLevel);
            return entry;
          })
  );

  private final String key;
  private final Map<String, Integer> ingredients;
  private final String npcName;
  private final Long storedAt;

  // Conditions
  /**
   * -1: Not applicable, 0: Not known, 1: Known
   * e.g. "✖ Recipe Known"
   */
  @Setter
  private byte recipeKnown;

  /**
   * e.g. "✔ Requires 250.0 Coin"
   */
  @Setter
  private float cost;

  /**
   * e.g. "✔ Requires 10 in Runecarving"
   */
  @Setter
  private String requiredClass;

  /**
   * e.g. "✔ Requires 10 in Runecarving"
   */
  @Setter
  private int requiredLevel;

  private ItemStack knownItem;

  public JournalEntry(
      String key, Map<String, Integer> ingredients, String npcName, Long storedAt) {
    this.key = key;
    this.ingredients = ingredients;
    this.npcName = npcName;
    this.storedAt = storedAt;

    this.recipeKnown = -1;
    this.cost = -1.0f;
    this.requiredClass = "";
    this.requiredLevel = -1;
  }

  public @Nullable ItemStack getItem() {
    if (Journal.INSTANCE == null) {
      return null;
    }

    if (this.knownItem == null) {
      this.knownItem = Journal.INSTANCE.getKnownItem(this.key);
    }

    return this.knownItem;
  }

  public List<ItemStack> getIngredientItems() {
    if (Journal.INSTANCE == null) {
      return List.of();
    }

    List<ItemStack> items = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : this.ingredients.entrySet()) {
      ItemStack item = Journal.INSTANCE.getKnownItem(entry.getKey());
      if (item != null) {
        item.setCount(entry.getValue());
        items.add(item);
      }
    }

    return items;
  }
}
