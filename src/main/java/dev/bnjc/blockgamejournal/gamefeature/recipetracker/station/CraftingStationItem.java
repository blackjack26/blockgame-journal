package dev.bnjc.blockgamejournal.gamefeature.recipetracker.station;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class CraftingStationItem {
  private final ItemStack originalItem;
  private final ItemStack item;
  private final int slot;

  @Setter
  @Nullable
  private Boolean recipeKnown = null;

  @Setter
  private float cost = -1;

  @Setter
  private String requiredClass = "";

  @Setter
  private int requiredLevel = -1;

  @Setter
  @Nullable
  private List<Text> outdated = null;

  /**
   * This is used specifically for spot checking the ingredients of a recipe. We don't get the actual item stack here
   * but rather just a string representation formatted like "48 Amethyst Shard" where 48 is the amount of the item.
   * <br/><br/>
   * <i>NOTE: This shouldn't be stored in the journal, as it is only used to validate the ingredients of a recipe when
   * it is being stored in the journal.</i>
   */
  private final Map<String, Integer> expectedIngredients;

  public CraftingStationItem(ItemStack item, int slot) {
    this.originalItem = item;
    this.item = item.copy();
    this.slot = slot;
    this.expectedIngredients = new HashMap<>();
  }

  public void addExpectedIngredient(String ingredient, int amount) {
    this.expectedIngredients.put(ingredient, amount);
  }

  public CraftingStationItem copy() {
    CraftingStationItem copy = new CraftingStationItem(this.originalItem, this.slot);
    copy.setRecipeKnown(this.recipeKnown);
    copy.setCost(this.cost);
    copy.setRequiredClass(this.requiredClass);
    copy.setRequiredLevel(this.requiredLevel);
    copy.setOutdated(this.outdated);
    this.expectedIngredients.forEach(copy::addExpectedIngredient);
    return copy;
  }
}
