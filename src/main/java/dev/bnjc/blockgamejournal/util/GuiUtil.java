package dev.bnjc.blockgamejournal.util;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import net.minecraft.util.Identifier;

public class GuiUtil {
  public static Identifier sprite(String path) {
    return new Identifier(BlockgameJournal.MOD_ID, path);
  }
}
