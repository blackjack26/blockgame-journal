package dev.bnjc.blockgamejournal.util;

import com.mojang.serialization.Codec;
import dev.bnjc.blockgamejournal.BlockgameJournal;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class FileUtil {
  public static final Logger LOGGER = BlockgameJournal.getLogger("FileUtil");

  public static <T> boolean saveToNbt(T object, Codec<T> codec, Path path) {
    try {
      Files.createDirectories(path.getParent());
      var tag = codec.encodeStart(NbtOps.INSTANCE, object).get();
      var result = tag.left();
      var err = tag.right();
      if (err.isPresent()) {
        throw new IOException("Error encoding to NBT %s".formatted(err.get()));
      }

      if (result.isPresent() && result.get() instanceof NbtCompound compoundTag) {
        NbtIo.writeCompressed(compoundTag, path.toFile());
        return true;
      }

      throw new IOException("Error encoding to NBT: %s".formatted(result));
    } catch (IOException ex) {
      LOGGER.error("[Blockgame Journal] Error saving NBT to {}", path, ex);
      return false;
    }
  }

  public static <T> Optional<T> loadFromNbt(Codec<T> codec, Path path) {
    if (Files.isRegularFile(path)) {
      try {
        FileInputStream stream = new FileInputStream(path.toFile());
        NbtCompound tag = NbtIo.readCompressed(stream);
        var loaded = codec.decode(NbtOps.INSTANCE, tag).get();
        if (loaded.right().isPresent()) {
          throw new IOException("Invalid NBT: %s".formatted(loaded.right().get()));
        } else {
          return Optional.ofNullable(loaded.left().get().getFirst());
        }
      } catch (IOException ex) {
        LOGGER.error("[Blockgame Journal] Error loading NBT from {}", path, ex);
        FileUtil.tryMove(path, path.resolveSibling(path.getFileName() + ".corrupted"), StandardCopyOption.REPLACE_EXISTING);
      }
    }
    return Optional.empty();
  }

  public static void tryMove(Path from, Path to, CopyOption... options) {
    try {
      Files.move(from, to, options);
    } catch (IOException ex) {
      LOGGER.error("[Blockgame Journal] Error moving file from {} to {}", from, to, ex);
    }
  }
}
