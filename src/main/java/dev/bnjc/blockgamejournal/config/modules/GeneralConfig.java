package dev.bnjc.blockgamejournal.config.modules;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "general")
public class GeneralConfig implements ConfigData {
  @ConfigEntry.Gui.Tooltip
  public boolean decomposeVanillaItems;

  public GeneralConfig() {
    decomposeVanillaItems = false;
  }
}