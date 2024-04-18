package dev.bnjc.blockgamejournal.storage.backend;

import com.mojang.authlib.GameProfile;
import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.metadata.Metadata;
import dev.bnjc.blockgamejournal.journal.npc.NPCEntry;
import dev.bnjc.blockgamejournal.util.FileUtil;
import dev.bnjc.blockgamejournal.util.Timer;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.*;

public class NbtBackend extends FileBasedBackend {
  private static final Logger LOGGER = BlockgameJournal.getLogger("NBT");

  @Override
  public @Nullable Journal load() {
    Optional<Metadata> meta = this.loadMetadata();
    if (meta.isEmpty()) {
      return null;
    }

    var metadata = meta.get();
    var entries = this.loadJournalEntries();
    var items = this.loadKnownItems();
    var npcs = this.loadKnownNPCs();

    if (npcs.isEmpty()) {
      // Check for legacy NPCs
      var legacyNPCs = this.loadLegacyNPCs();
      if (!legacyNPCs.isEmpty()) {
        LOGGER.info("[Blockgame Journal] Migrating legacy NPCs to new format");
        for (var entry : legacyNPCs.entrySet()) {
          npcs.put(entry.getKey(), NPCEntry.fromLegacy(entry.getKey(), entry.getValue()));
        }
      }
    }

    return new Journal(metadata, entries, items, npcs);
  }

  @Override
  public boolean save(Journal journal) {
    LOGGER.info("[Blockgame Journal] Saving {} journal entries to {}", journal.getEntries().size(), STORAGE_DIR);

    journal.getMetadata().updateLastModified();
    if (!this.saveMetadata(journal.getMetadata())) {
      LOGGER.error("[Blockgame Journal] Failed to save metadata");
      return false;
    }

    boolean result = FileUtil.saveToNbt(journal.getEntries(), Journal.JOURNAL_CODEC, STORAGE_DIR.resolve(JOURNAL_NAME + extension()));
    result &= FileUtil.saveToNbt(journal.getKnownItems(), Journal.KNOWN_ITEMS_CODEC, STORAGE_DIR.resolve(KNOWN_ITEMS_NAME + extension()));
    result &= FileUtil.saveToNbt(journal.getKnownNPCs(), Journal.KNOWN_NPCS_CODEC, STORAGE_DIR.resolve(NPC_CACHE_NAME + extension()));

    // Remove legacy NPC cache if it exists
    this.removeLegacyNPCs();

    return result;
  }

  @Override
  public String extension() {
    return ".nbt";
  }

  private Map<String, ArrayList<JournalEntry>> loadJournalEntries() {
    Path journalPath = STORAGE_DIR.resolve(JOURNAL_NAME + extension());
    var result = Timer.time(() -> FileUtil.loadFromNbt(Journal.JOURNAL_CODEC, journalPath));
    if (result.getFirst().isPresent()) {
      LOGGER.info("[Blockgame Journal] Loaded entries {} in {}ns", journalPath, result.getSecond());
      return result.getFirst().get();
    }

    LOGGER.warn("[Blockgame Journal] Failed to load journal entries from {}", journalPath);
    return new HashMap<>();
  }

  private Map<String, ItemStack> loadKnownItems() {
    // Load cached items if they exist
    Path itemsPath = STORAGE_DIR.resolve(KNOWN_ITEMS_NAME + extension());
    var itemsResult = Timer.time(() -> FileUtil.loadFromNbt(Journal.KNOWN_ITEMS_CODEC, itemsPath));
    if (itemsResult.getFirst().isPresent()) {
      LOGGER.info("[Blockgame Journal] Loaded items {} in {}ns", itemsPath, itemsResult.getSecond());
      return itemsResult.getFirst().get();
    }

    LOGGER.warn("[Blockgame Journal] Failed to load cached items from {}", itemsPath);
    return new HashMap<>();
  }

  private Map<String, NPCEntry> loadKnownNPCs() {
    // Load cached NPCs if they exist
    Path npcPath = STORAGE_DIR.resolve(NPC_CACHE_NAME + extension());
    var npcResult = Timer.time(() -> FileUtil.loadFromNbt(Journal.KNOWN_NPCS_CODEC, npcPath));
    if (npcResult.getFirst().isPresent()) {
      LOGGER.info("[Blockgame Journal] Loaded NPCs {} in {}ns", npcPath, npcResult.getSecond());
      return npcResult.getFirst().get();
    }

    return new HashMap<>();
  }

  @Deprecated(since = "0.2.0-alpha", forRemoval = true)
  private Map<String, GameProfile> loadLegacyNPCs() {
    // Load cached NPCs if they exist
    Path npcPath = STORAGE_DIR.resolve(NPC_LEGACY_CACHE_NAME + extension());
    var npcResult = Timer.time(() -> FileUtil.loadFromNbt(Journal.LEGACY_NPCS_CODE, npcPath));
    if (npcResult.getFirst().isPresent()) {
      LOGGER.info("[Blockgame Journal] Loaded NPCs {} in {}ns", npcPath, npcResult.getSecond());
      return npcResult.getFirst().get();
    }

    return new HashMap<>();
  }

  private void removeLegacyNPCs() {
    Path legacyNpcPath = STORAGE_DIR.resolve(NPC_LEGACY_CACHE_NAME + extension());
    if (FileUtil.deleteIfExists(legacyNpcPath)) {
      LOGGER.info("[Blockgame Journal] Removed legacy NPC cache");
    }
  }
}
