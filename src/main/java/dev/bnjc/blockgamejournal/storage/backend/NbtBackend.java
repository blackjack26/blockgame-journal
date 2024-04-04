package dev.bnjc.blockgamejournal.storage.backend;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.metadata.Metadata;
import dev.bnjc.blockgamejournal.util.FileUtil;
import dev.bnjc.blockgamejournal.util.Timer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public class NbtBackend extends FileBasedBackend {
  private static final Logger LOGGER = BlockgameJournal.getLogger("NBT");

  @Override
  public @Nullable Journal load() {
    Optional<Metadata> meta = this.loadMetadata();
    if (meta.isEmpty()) {
      return null;
    }

    // Load journal entries if they exist
    Path journalPath = STORAGE_DIR.resolve(JOURNAL_NAME + extension());
    var result = Timer.time(() -> FileUtil.loadFromNbt(Journal.JOURNAL_CODEC, journalPath));
    if (result.getFirst().isPresent()) {
      LOGGER.info("[Blockgame Journal] Loaded {} in {}ns", journalPath, result.getSecond());

      // Load cached items if they exist
      Path itemsPath = STORAGE_DIR.resolve(KNOWN_ITEMS_NAME + extension());
      var itemsResult = Timer.time(() -> FileUtil.loadFromNbt(Journal.KNOWN_ITEMS_CODEC, itemsPath));
      if (itemsResult.getFirst().isPresent()) {
        LOGGER.info("[Blockgame Journal] Loaded {} in {}ns", itemsPath, itemsResult.getSecond());
        return new Journal(meta.get(), result.getFirst().get(), itemsResult.getFirst().get());
      }

      LOGGER.warn("[Blockgame Journal] Failed to load cached items from {}", itemsPath);
      return new Journal(meta.get(), result.getFirst().get(), new HashMap<>());
    }

    return new Journal(meta.get(), new HashMap<>(), new HashMap<>());
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
    return result;
  }

  @Override
  public String extension() {
    return ".nbt";
  }
}
