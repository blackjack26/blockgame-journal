package dev.bnjc.blockgamejournal.storage.backend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.journal.metadata.Metadata;
import dev.bnjc.blockgamejournal.util.FileUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public abstract class FileBasedBackend implements Backend {
  public static final Logger LOGGER = BlockgameJournal.getLogger("File Storage");
  public static final Path STORAGE_DIR = FabricLoader.getInstance().getGameDir().resolve("blockgamejournal");

  public static final String JOURNAL_NAME = "journal";
  public static final String KNOWN_ITEMS_NAME = "item_cache";
  @Deprecated(since = "0.2.0-alpha", forRemoval = true)
  public static final String NPC_LEGACY_CACHE_NAME = "npc_cache";
  public static final String NPC_CACHE_NAME = "visited_npc";

  @Override
  public void delete() {
    getRelevantPaths().forEach(path -> {
      if (Files.isRegularFile(path)) {
        try {
          Files.delete(path);
          LOGGER.info("[Blockgame Journal] Deleted file {}", path);
        } catch (IOException e) {
          LOGGER.error("[Blockgame Journal] Failed to delete file {}", path, e);
        }
      }
    });
  }

  @Override
  public boolean saveMetadata(Metadata metadata) {
    Path path = STORAGE_DIR.resolve(JOURNAL_NAME + metadataExtension());
    try {
      Files.createDirectories(path.getParent());
      Optional<JsonElement> metaJson = Metadata.CODEC.encodeStart(JsonOps.INSTANCE, metadata).resultOrPartial(Util.addPrefix("Error encoding metadata", LOGGER::error));
      if (metaJson.isPresent()) {
        FileUtils.write(path.toFile(), new GsonBuilder().create().toJson(metaJson.get()), StandardCharsets.UTF_8);
        return true;
      } else {
        LOGGER.error("[Blockgame Journal] Unknown error encoding metadata");
      }
    } catch (IOException e) {
      LOGGER.error("[Blockgame Journal] Failed to save metadata to {}", path, e);
    }
    return false;
  }

  @Override
  public Optional<Metadata> loadMetadata() {
    Path path = STORAGE_DIR.resolve(JOURNAL_NAME + metadataExtension());
    if (Files.isRegularFile(path)) {
      try {
        String str = Files.readString(path, StandardCharsets.UTF_8);
        JsonElement json = new Gson().fromJson(str, JsonElement.class);
        AtomicReference<Metadata> metadata = new AtomicReference<>();
        Metadata.CODEC.decode(JsonOps.INSTANCE, json)
            .resultOrPartial(Util.addPrefix("Error decoding metadata", LOGGER::error))
            .ifPresent(pair -> metadata.set(pair.getFirst()));
        if (metadata.get() != null) {
          return Optional.ofNullable(metadata.get());
        }
      } catch (JsonParseException | IOException e) {
        LOGGER.error("[Blockgame Journal] Error decoding metadata", e);
        FileUtil.tryMove(path, path.resolveSibling(path.getFileName() + ".corrupted"), StandardCopyOption.REPLACE_EXISTING);
      }
    }

    return Optional.empty();
  }

  public abstract String extension();

  protected String metadataExtension() {
    return extension() + ".meta";
  }

  protected List<Path> getRelevantPaths() {
    return List.of(
        STORAGE_DIR.resolve(JOURNAL_NAME + extension()),
        STORAGE_DIR.resolve(JOURNAL_NAME + metadataExtension())
    );
  }
}
