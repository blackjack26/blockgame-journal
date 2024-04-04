package dev.bnjc.blockgamejournal.storage;

import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.metadata.Metadata;
import net.minecraft.client.gui.screen.GameMenuScreen;
import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.storage.backend.Backend;
import lombok.Setter;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;

public class Storage {
  private static final Logger LOGGER = BlockgameJournal.getLogger("Storage");

  @Setter
  private static Backend backend;

  public static void setup() {
    BlockgameJournal.getConfig().getStorageConfig().backendType.load();

    // On Pause
    ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
      if (screen instanceof GameMenuScreen) {
        Journal.save();
      }
    });
  }

  // API
  public static Optional<Metadata> loadMetadata() {
    if (Journal.INSTANCE != null) {
      return Optional.of(Journal.INSTANCE.getMetadata().deepCopy());
    }

    LOGGER.debug("[Blockgame Journal] Loading metadata using {}", backend.getClass().getSimpleName());
    return backend.loadMetadata();
  }

  public static void delete() {
    backend.delete();
  }

  public static Optional<Journal> load() {
    if (Journal.INSTANCE != null) {
      return Optional.of(Journal.INSTANCE);
    }

    LOGGER.debug("[Blockgame Journal] Loading journal using {}", backend.getClass().getSimpleName());
    Journal loaded = backend.load();
    if (loaded == null) {
      return Optional.empty();
    }

    return Optional.of(loaded);
  }

  public static void save(@Nullable Journal journal) {
    if (journal == null) {
      LOGGER.warn("[Blockgame Journal] Journal is null, not saving");
      return;
    }

    journal.getMetadata().updateLastModified();
    boolean saved = backend.save(journal);
    if (!saved) {
      LOGGER.error("[Blockgame Journal] Failed to save journal");
    }
  }
}
