package dev.bnjc.blockgamejournal.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public final class JournalEntry {
  public static final Codec<JournalEntry> CODEC = RecordCodecBuilder.create(instance ->
          instance.group(
              Codec.STRING.fieldOf("key").forGetter(JournalEntry::getKey),
              Codec.INT.fieldOf("count").forGetter(JournalEntry::getCount),
              Codec.INT.fieldOf("revisionId").forGetter(JournalEntry::getRevisionId),
              Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("ingredients").forGetter(JournalEntry::getIngredients),
              Codec.STRING.fieldOf("npcName").forGetter(JournalEntry::getNpcName),
              Codec.INT.fieldOf("slot").forGetter(JournalEntry::getSlot),
              Codec.LONG.fieldOf("storedAt").forGetter(JournalEntry::getStoredAt),
              Codec.BYTE.orElse((byte) -1).fieldOf("recipeKnown").forGetter(JournalEntry::getRecipeKnown),
              Codec.FLOAT.orElse(-1.0f).fieldOf("cost").forGetter(JournalEntry::getCost),
              Codec.STRING.orElse("").fieldOf("requiredClass").forGetter(JournalEntry::getRequiredClass),
              Codec.INT.orElse(-1).fieldOf("requiredLevel").forGetter(JournalEntry::getRequiredLevel),
              Codec.BOOL.optionalFieldOf("favorite", false).forGetter(JournalEntry::isFavorite),
              Codec.BOOL.optionalFieldOf("unavailable", false).forGetter(JournalEntry::isUnavailable)
          ).apply(instance, (key, count, revisionId, ingredients, npcName, slot, storedAt, recipeKnown, cost, requiredClass, requiredLevel, favorite, unavailable) -> {
            JournalEntry entry = new JournalEntry(key, count, revisionId, ingredients, npcName, slot, storedAt);
            entry.setRecipeKnown(recipeKnown);
            entry.setCost(cost);
            entry.setRequiredClass(requiredClass);
            entry.setRequiredLevel(requiredLevel);
            entry.setFavorite(favorite);
            entry.setUnavailable(unavailable);
            return entry;
          })
  );

  /**
   * The item identifier. e.g. <code>"mmoitems:ESSENCE_CORRUPTED"</code>
   */
  private final String key;

  /**
   * The item stack count of the item represented by the {@link #key}.
   */
  private final int count;
  /**
   * The revision identifier of the item. This is used to determine if the item has been updated after it was stored
   * in the journal.
   */
  private final int revisionId;

  /**
   * The ingredients required to craft the item. The key is the item identifier and the value is the amount of the item.
   */
  private final Map<String, Integer> ingredients;

  /**
   * The name of the NPC that the item can be crafted at.
   */
  @Setter
  private String npcName;

  /**
   * The slot in the inventory where the item was stored.
   */
  private final int slot;

  /**
   * The timestamp when the item was stored in the journal.
   */
  private final Long storedAt;

  // Conditions
  /**
   * Whether the recipe is known or not. The value is one of the following:
   * -1=Not applicable, 0=Not known, 1=Known
   */
  @Setter
  private byte recipeKnown;

  /**
   * The cost to craft the item.
   */
  @Setter
  private float cost;

  /**
   * The required class to craft the item. e.g. "Runecarving"
   */
  @Setter
  private String requiredClass;

  /**
   * The required level of {@link #requiredClass} to craft the item.
   */
  @Setter
  private int requiredLevel;

  @Setter
  private boolean favorite;

  @Setter
  private boolean unavailable;

  private ItemStack knownItem;

  public JournalEntry(ItemStack stack, Map<String, Integer> ingredients, String npcName, int slot, Long storedAt) {
    this(ItemUtil.getKey(stack), stack.getCount(), ItemUtil.getRevisionId(stack).orElse(-1), ingredients, npcName, slot, storedAt);
  }

  public JournalEntry(String key, int count, int revisionId, Map<String, Integer> ingredients, String npcName, int slot, Long storedAt) {
    this.key = key;
    this.count = count;
    this.revisionId = revisionId;
    this.ingredients = ingredients;
    this.npcName = npcName;
    this.slot = slot;
    this.storedAt = storedAt;

    this.recipeKnown = -1;
    this.cost = -1.0f;
    this.requiredClass = "";
    this.requiredLevel = -1;

    this.favorite = false;
    this.unavailable = false;
  }

  public @Nullable ItemStack getItem() {
    if (Journal.INSTANCE == null) {
      return null;
    }

    if (this.knownItem == null) {
      this.knownItem = Journal.INSTANCE.getKnownItem(this.key);
      if (this.knownItem != null) {
        this.knownItem.setCount(this.count);
      }
    }

    return this.knownItem;
  }

  public List<ItemStack> getIngredientItems() {
    if (Journal.INSTANCE == null) {
      return List.of();
    }

    List<ItemStack> items = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : this.ingredients.entrySet()) {
      ItemStack item = null;

      if (entry.getKey().startsWith("mmoitems:")) {
        item = Journal.INSTANCE.getKnownItem(entry.getKey());
      }
      else if (entry.getKey().startsWith("minecraft:")) {
        item = new ItemStack(Registries.ITEM.get(new Identifier(entry.getKey())));
      }

      if (item != null) {
        item.setCount(entry.getValue());
        items.add(item);
      }
    }

    return items;
  }

  public void toggleFavorite() {
    this.favorite = !this.favorite;
  }

  public DecomposedJournalEntry decompose() {
    return DecomposedJournalEntry.decompose(this);
  }
}
