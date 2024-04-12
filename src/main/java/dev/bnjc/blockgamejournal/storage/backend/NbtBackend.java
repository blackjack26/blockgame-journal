package dev.bnjc.blockgamejournal.storage.backend;

import com.mojang.authlib.GameProfile;
import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.metadata.Metadata;
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

  private Map<String, GameProfile> loadKnownNPCs() {
    // Load cached NPCs if they exist
    Path npcPath = STORAGE_DIR.resolve(NPC_CACHE_NAME + extension());
    var npcResult = Timer.time(() -> FileUtil.loadFromNbt(Journal.KNOWN_NPCS_CODEC, npcPath));
    if (npcResult.getFirst().isPresent()) {
      LOGGER.info("[Blockgame Journal] Loaded NPCs {} in {}ns", npcPath, npcResult.getSecond());
      return npcResult.getFirst().get();
    }

    LOGGER.warn("[Blockgame Journal] Failed to load cached NPCs from {}", npcPath);
    return new HashMap<>();
  }
}
