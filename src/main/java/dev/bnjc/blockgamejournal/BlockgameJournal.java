package dev.bnjc.blockgamejournal;

import dev.bnjc.blockgamejournal.config.modules.ModConfig;
import lombok.Getter;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockgameJournal implements ModInitializer {
  public static final Logger LOGGER = LoggerFactory.getLogger("BlockgameJournal");
  public static final boolean DEBUG = System.getenv("bgj-debug") != null;
  public static final String MOD_ID = "blockgamejournal";

  @Getter
  private static boolean modMenuPresent = false;

  @Getter
  private static ModConfig config;

  @Override
  public void onInitialize() {
    // Cloth Config
    AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
    config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

    // Detect if the ModMenu mod is present.
    if (FabricLoader.getInstance().isModLoaded("modmenu")) {
      modMenuPresent = true;
    }
  }

  public static Logger getLogger(String suffix) {
    return LoggerFactory.getLogger(BlockgameJournal.class.getCanonicalName() + "/" + suffix);
  }
}
