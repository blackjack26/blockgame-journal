package dev.bnjc.blockgamejournal.journal.npc;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class NPCNames {
  private static final Map<String, NPCName> NAMES;
  static {
    NAMES = new HashMap<>();
    NAMES.put("Alchemist", new NPCName("Nina", "Alchemist"));
    NAMES.put("Indy", new NPCName("Indy", "Archaeologist"));
    NAMES.put("Mortimer", new NPCName("Mortimer", "Archaeologist"));
    NAMES.put("Eitri", new NPCName("Eitri", "Artificer"));
    NAMES.put("Blacksmith", new NPCName("Markus", "Blacksmith"));
    NAMES.put("Hesha", new NPCName("Hesha", "Botanist"));
    NAMES.put("Mint", new NPCName("Mint", "Botanist"));
    NAMES.put("Bowyer", new NPCName("Brent", "Bowyer"));
    NAMES.put("Baron Warbucks", new NPCName("Baron Warbucks", "Warlord"));
    NAMES.put("Chef Holiday", new NPCName("Holiday", "Chef"));
    NAMES.put("Chef Ken", new NPCName("Ken", "Chef"));
    NAMES.put("Chef Sue", new NPCName("Sue", "Chef"));
    NAMES.put("Chef Nugget", new NPCName("Nugget", "Chef"));
    NAMES.put("Chef Axel Otto", new NPCName("Axel Otto", "Chef"));
    NAMES.put("Jam Master", new NPCName("Jam Master", "Master of Jams"));
    NAMES.put("Baelin", new NPCName("Baelin", "Fisherman"));
    NAMES.put("Franky", new NPCName("Franky", "Fisherman"));
    NAMES.put("Guardian", new NPCName("Armand", "Guardian"));
    NAMES.put("Amun", new NPCName("Amun", null));
    NAMES.put("Heptet", new NPCName("Heptet", null));
    NAMES.put("Hunter", new NPCName("Hanzo", "Hunter"));
    NAMES.put("Leatherworker", new NPCName("Seymour", "Leatherworker"));
    NAMES.put("Larry", new NPCName("Larry", "Lumberjack"));
    NAMES.put("Paul", new NPCName("Paul", "Lumberjack"));
    NAMES.put("Potion Seller", new NPCName("Justin", "Potion Seller"));
    NAMES.put("Metal", new NPCName("Metal", "Golem"));
    NAMES.put("Smith", new NPCName("Smith", "Repairman"));
    NAMES.put("Forge", new NPCName("Forge", "Potato Guard"));
    NAMES.put("George", new NPCName("George", "Miner"));
    NAMES.put("Steve", new NPCName("Steve", "Miner"));
    NAMES.put("Brokkr", new NPCName("Brokkr", "Rune Carver"));
    NAMES.put("Stonebeard", new NPCName("Stonebeard", "Rune Carver"));
    NAMES.put("Runehilda", new NPCName("Runehilda", "Rune Carver"));
    NAMES.put("Silk Weaver", new NPCName("Wendy", "Silk Weaver"));
    NAMES.put("Spellcrafter", new NPCName("Sally", "Spellcrafter"));
    NAMES.put("Sooie Casa", new NPCName("Sooie Casa", "Shady Official"));
    NAMES.put("Stone", new NPCName("Stone", "Golem"));
    NAMES.put("Tea Master", new NPCName("Piggly Wiggly", "Tea Master"));
    NAMES.put("Thaumaturge", new NPCName("Gregory", "Thaumaturge"));
    NAMES.put("Warrior", new NPCName("Willy", "Warrior"));
    NAMES.put("Wood", new NPCName("Wood", "Golem"));
    NAMES.put("Wool Weaver", new NPCName("Porfirio", "Wool Weaver"));

    NAMES.put("Myrkheim Dealer", new NPCName("Myrkheim Dealer", "Unreliable Guide"));
    NAMES.put("Sunken Dealer", new NPCName("Sunken Dealer", null));
  }

  private NPCNames() {}

  public static NPCName get(String name) {
    return NAMES.getOrDefault(name, new NPCName(name, null));
  }

  public record NPCName(String name, @Nullable String title) {}
}
