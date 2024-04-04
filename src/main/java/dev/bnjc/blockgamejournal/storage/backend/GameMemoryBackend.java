package dev.bnjc.blockgamejournal.storage.backend;

import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.metadata.Metadata;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Optional;

/**
 * A backend that stores data in memory for the duration of the game session.
 */
public class GameMemoryBackend implements Backend {
  @Nullable
  private Journal journal = null;

  @Override
  public @Nullable Journal load() {
    return journal;
  }

  @Override
  public void delete() {
    journal = null;
  }

  @Override
  public boolean save(Journal journal) {
    this.journal = journal;
    return true;
  }

  @Override
  public Optional<Metadata> loadMetadata() {
    if (journal == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(journal.getMetadata());
  }

  @Override
  public boolean saveMetadata(Metadata metadata) {
    if (journal == null) {
      this.journal = new Journal(metadata, new HashMap<>(), new HashMap<>());
    } else {
      this.journal.setMetadata(metadata);
    }
    return true;
  }
}
