package dev.bnjc.blockgamejournal.config.modules;

import lombok.Getter;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;

@Config(name = "blockgamejournal")
public class ModConfig extends PartitioningSerializer.GlobalData {
  @Getter
  @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
  GeneralConfig generalConfig = new GeneralConfig();

  @Getter
  @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
  StorageConfig storageConfig = new StorageConfig();
}
