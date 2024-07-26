package dev.bnjc.blockgamejournal.journal.npc;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class NPCNames {
  private static final Map<String, NPCName> NAMES;
  static {
    NAMES = new HashMap<>();
    NAMES.put("Alchemist", new NPCName("Nina", "Alchemist"));
    NAMES.put("Archaeologist", new NPCName("Indy", "Archaeologist"));
    NAMES.put("Mortimer", new NPCName("Mortimer", "Archaeologist"));
    NAMES.put("Artificer", new NPCName("Eitri", "Artificer"));
    NAMES.put("Blacksmith", new NPCName("Markus", "Blacksmith"));
    NAMES.put("Botanist", new NPCName("Hesha", "Botanist"));
    NAMES.put("Mint", new NPCName("Mint", "Botanist"));
    NAMES.put("Bowyer", new NPCName("Brent", "Bowyer"));
    NAMES.put("Chef Holiday", new NPCName("Holiday", "Chef"));
    NAMES.put("Chef Ken", new NPCName("Ken", "Chef"));
    NAMES.put("Chef Sue", new NPCName("Sue", "Chef"));
    NAMES.put("Jam Master", new NPCName("Jam Master", null));
    NAMES.put("Baelin", new NPCName("Baelin", "Fisherman"));
    NAMES.put("Fisherman", new NPCName("Franky", "Fisherman"));
    NAMES.put("Guardian", new NPCName("Armand", "Guardian"));
    NAMES.put("Hunter", new NPCName("Hanzo", "Hunter"));
    NAMES.put("Leatherworker", new NPCName("Seymour", "Leatherworker"));
    NAMES.put("Lumberjack", new NPCName("Larry", "Lumberjack"));
    NAMES.put("Paul", new NPCName("Paul", "Lumberjack"));
    NAMES.put("Metal", new NPCName("Metal", "Golem"));
    NAMES.put("George", new NPCName("George", "Miner"));
    NAMES.put("Miner", new NPCName("Steve", "Miner"));
    NAMES.put("Rune Carver", new NPCName("Brokkr", "Rune Carver"));
    NAMES.put("Stonebeard", new NPCName("Stonebeard", "Rune Carver"));
    NAMES.put("Silk Weaver", new NPCName("Wendy", "Silk Weaver"));
    NAMES.put("Spellcrafter", new NPCName("Sally", "Spellcrafter"));
    NAMES.put("Stone", new NPCName("Stone", "Golem"));
    NAMES.put("Tea Master", new NPCName("Piggly Wiggly", "Tea Master"));
    NAMES.put("Thaumaturge", new NPCName("Gregory", "Thaumaturge"));
    NAMES.put("Warrior", new NPCName("Willy", "Warrior"));
    NAMES.put("Wood", new NPCName("Wood", "Golem"));
    NAMES.put("Wool Weaver", new NPCName("Porfirio", "Wool Weaver"));
  }

  private NPCNames() {}

  public static NPCName get(String name) {
    return NAMES.getOrDefault(name, new NPCName(name, null));
  }

  public record NPCName(String name, @Nullable String title) {}
}
