package dev.bnjc.blockgamejournal.journal;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

@Getter
public class KnownItem {
  private final ItemStack item;

  @Setter
  private byte recipeKnown = -1;

  @Setter
  private float cost = -1;

  @Setter
  private String requiredClass = "";

  @Setter
  private int requiredLevel = -1;

  /**
   * This is used specifically for spot checking the ingredients of a recipe. We don't get the actual item stack here
   * but rather just a string representation formatted like "48 Amethyst Shard" where 48 is the amount of the item.
   * <br/><br/>
   * <i>NOTE: This shouldn't be stored in the journal, as it is only used to validate the ingredients of a recipe when
   * it is being stored in the journal.</i>
   */
  private final Map<String, Integer> expectedIngredients;

  public KnownItem(ItemStack item) {
    this.item = item;
    this.expectedIngredients = new HashMap<>();
  }
}
