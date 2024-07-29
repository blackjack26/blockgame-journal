package dev.bnjc.blockgamejournal.journal.npc;

import dev.bnjc.blockgamejournal.journal.Journal;
import net.minecraft.entity.Entity;

import java.util.HashSet;
import java.util.Set;

public final class NPCUtil {
  public static void createOrUpdate(String name, Entity entity) {
    if (Journal.INSTANCE == null) {
      return;
    }

    NPCEntry npcEntry = NPCEntry.of(entity);

    // Check if there is an existing NPC entry with the same name
    NPCEntry existing = Journal.INSTANCE.getKnownNPCs().get(name);
    if (existing == null) {
      // Check if there is an existing NPC entry with the same UUID
      for (var knownNpcEntry : Journal.INSTANCE.getKnownNPCs().entrySet()) {
        if (knownNpcEntry.getValue().getId().equals(npcEntry.getId())) {
          // This means the NPC has changed names, we need to store with the new name and remove the old entry.
          Journal.INSTANCE.getKnownNPCs().remove(knownNpcEntry.getKey());

          // We also need to update the "npcName" field in all journal entries that reference the old name.
          for (var entry : Journal.INSTANCE.getEntries().values()) {
            for (var journalEntry : entry) {
              if (journalEntry.getNpcName().equals(knownNpcEntry.getKey())) {
                journalEntry.setNpcName(name);
              }
            }
          }

          break;
        }
      }
    }

    // Add or update the NPC entry
    Journal.INSTANCE.getKnownNPCs().put(name, npcEntry);
  }

  public static void removeNPC(String name) {
    if (Journal.INSTANCE == null) {
      return;
    }

    Journal.INSTANCE.getKnownNPCs().remove(name);

    // Remove all entries that reference the NPC
    Set<String> keysToRemove = new HashSet<>();
    for (var entrySet : Journal.INSTANCE.getEntries().entrySet()) {
      entrySet.getValue().removeIf(journalEntry -> journalEntry.getNpcName().equals(name));

      // Remove the entry if it is empty
      if (entrySet.getValue().isEmpty()) {
        keysToRemove.add(entrySet.getKey());
      }
    }

    keysToRemove.forEach(Journal.INSTANCE.getEntries()::remove);
  }
}
