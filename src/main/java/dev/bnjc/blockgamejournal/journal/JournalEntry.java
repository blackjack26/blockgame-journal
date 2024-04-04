package dev.bnjc.blockgamejournal.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;

import java.util.Map;

@Getter
public final class JournalEntry {
  public static final Codec<JournalEntry> CODEC = RecordCodecBuilder.create(instance ->
          instance.group(
              Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("ingredients").forGetter(JournalEntry::getIngredients),
              Codec.INT.fieldOf("npcId").forGetter(JournalEntry::getNpcId),
              Codec.STRING.fieldOf("npcName").forGetter(JournalEntry::getNpcName),
              Codec.LONG.fieldOf("storedAt").forGetter(JournalEntry::getStoredAt),
              Codec.BYTE.orElse((byte) -1).fieldOf("recipeKnown").forGetter(JournalEntry::getRecipeKnown),
              Codec.FLOAT.orElse(-1.0f).fieldOf("cost").forGetter(JournalEntry::getCost),
              Codec.STRING.orElse("").fieldOf("requiredClass").forGetter(JournalEntry::getRequiredClass),
              Codec.INT.orElse(-1).fieldOf("requiredLevel").forGetter(JournalEntry::getRequiredLevel)
          ).apply(instance, (ingredients, npcId, npcName, storedAt, recipeKnown, cost, requiredClass, requiredLevel) -> {
            JournalEntry entry = new JournalEntry(ingredients, npcId, npcName, storedAt);
            entry.setRecipeKnown(recipeKnown);
            entry.setCost(cost);
            entry.setRequiredClass(requiredClass);
            entry.setRequiredLevel(requiredLevel);
            return entry;
          })
  );

  private final Map<String, Integer> ingredients;
  private final int npcId;
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

  public JournalEntry(
      Map<String, Integer> ingredients, int npcId, String npcName, Long storedAt) {
    this.ingredients = ingredients;
    this.npcId = npcId;
    this.npcName = npcName;
    this.storedAt = storedAt;

    this.recipeKnown = -1;
    this.cost = -1.0f;
    this.requiredClass = "";
    this.requiredLevel = -1;
  }
}
