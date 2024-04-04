package dev.bnjc.blockgamejournal.storage.backend;

import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.metadata.Metadata;
import dev.bnjc.blockgamejournal.storage.Storage;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A handler for storing a memory bank
 */
public interface Backend {
  /**
   * Loads the journal from the backend if it exists, or returns null if not
   * @return Loaded journal or null if not available
   */
  @Nullable
  Journal load();

  /**
   * Deletes the journal from the storage. Not reversible.
   */
  void delete();

  /**
   * Saves this journal to the backend
   * @param journal Journal to save to this storage
   */
  boolean save(Journal journal);

  /**
   * Returns just the metadata of the journal, if it exists.
   * @return Metadata from the backend
   */
  Optional<Metadata> loadMetadata();

  boolean saveMetadata(Metadata metadata);

  enum Type {
    NBT(new NbtBackend()),
    MEMORY(new GameMemoryBackend());

    public final Backend instance;

    Type(Backend instance) {
      this.instance = instance;
    }

    public void load() {
      Journal.unload();
      Storage.setBackend(this.instance);
    }
  }
}
