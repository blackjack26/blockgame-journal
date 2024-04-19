package dev.bnjc.blockgamejournal.config.modules;

import lombok.Getter;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;

@Getter
@Config(name = "blockgamejournal")
@Config.Gui.Background("minecraft:textures/block/polished_blackstone_bricks.png")
public class ModConfig extends PartitioningSerializer.GlobalData {
  @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
  GeneralConfig generalConfig = new GeneralConfig();

  @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
  DecompositionConfig decompositionConfig = new DecompositionConfig();

  @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
  StorageConfig storageConfig = new StorageConfig();
}
