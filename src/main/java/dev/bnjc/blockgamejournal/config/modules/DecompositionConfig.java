package dev.bnjc.blockgamejournal.config.modules;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "decomposition")
public class DecompositionConfig implements ConfigData {
  @ConfigEntry.Gui.Tooltip
  public boolean decomposeVanillaItems;

  @ConfigEntry.Gui.Tooltip
  public boolean partialDecomposition;

  public DecompositionConfig() {
    decomposeVanillaItems = false;
    partialDecomposition = true;
  }
}
