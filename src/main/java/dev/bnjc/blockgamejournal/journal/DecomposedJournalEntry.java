package dev.bnjc.blockgamejournal.journal;

import lombok.Getter;

import java.util.*;

@Getter
public class DecomposedJournalEntry {
  private final String key;
  private float cost;
  private Map<String, Integer> ingredients;
  private Set<String> npcNames;
  private Map<String, Integer> professions;
  private Map<String, Byte> knownRecipes;

  public DecomposedJournalEntry(String key) {
    this.key = key;
    this.cost = 0.0f;
    this.ingredients = new HashMap<>();
    this.npcNames = new HashSet<>();
    this.professions = new HashMap<>();
    this.knownRecipes = new HashMap<>();
  }

  public static DecomposedJournalEntry decompose(JournalEntry entry) {
    DecomposedJournalEntry decomposed = new DecomposedJournalEntry(entry.getKey());
    parseJournalEntry(entry, decomposed, 1);
    return decomposed;
  }

  private static void parseJournalEntry(JournalEntry entry, DecomposedJournalEntry decomposed, int quantity) {
    if (Journal.INSTANCE == null) {
      return;
    }

    // Cost
    decomposed.cost += entry.getCost() * quantity;

    // NPC
    decomposed.npcNames.add(entry.getNpcName());

    // Required Profession
    if (entry.getRequiredClass() != null) {
      // Add the profession to the list
      // If the profession is already in the list, take the highest level
      decomposed.professions.merge(entry.getRequiredClass(), entry.getRequiredLevel(), Integer::max);
    }

    // Known Recipe
    if (entry.getRecipeKnown() != -1) {
      decomposed.knownRecipes.put(entry.getKey(), entry.getRecipeKnown());
    }

    // Ingredients
    for (Map.Entry<String, Integer> ingredient : entry.getIngredients().entrySet()) {
      List<JournalEntry> entries = Journal.INSTANCE.getEntries().get(ingredient.getKey());
      if (entries != null && !entries.isEmpty()) {
        // TODO: Handle multiple entries
        parseJournalEntry(entries.get(0), decomposed, ingredient.getValue() * quantity);
      } else {
        // If there is no entry for the ingredient, add it to the list
        // If the ingredient is already in the list, add the count
        decomposed.ingredients.merge(ingredient.getKey(), ingredient.getValue() * quantity, Integer::sum);
      }
    }
  }

  @Override
  public String toString() {
    return "DecomposedJournalEntry{" +
        "key='" + key + '\'' +
        ", cost=" + cost +
        ", ingredients=" + ingredients +
        ", npcNames=" + npcNames +
        ", professions=" + professions +
        ", knownRecipes=" + knownRecipes +
        '}';
  }
}
