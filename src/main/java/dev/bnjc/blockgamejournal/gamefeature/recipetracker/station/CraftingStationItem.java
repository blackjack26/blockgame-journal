package dev.bnjc.blockgamejournal.gamefeature.recipetracker.station;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

@Getter
public class CraftingStationItem {
  private final ItemStack item;
  private final int slot;

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

  public CraftingStationItem(ItemStack item, int slot) {
    this.item = item;
    this.slot = slot;
    this.expectedIngredients = new HashMap<>();
  }

  public void addExpectedIngredient(String ingredient, int amount) {
    this.expectedIngredients.put(ingredient, amount);
  }
}
