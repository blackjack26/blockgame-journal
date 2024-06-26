package dev.bnjc.blockgamejournal.config.modules;

import dev.bnjc.blockgamejournal.gui.widget.ItemListWidget;
import dev.bnjc.blockgamejournal.journal.JournalMode;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "general")
public class GeneralConfig implements ConfigData {
  @ConfigEntry.Gui.Tooltip
  public boolean highlightMissingRecipes;

  @ConfigEntry.Gui.Tooltip
  public boolean highlightOutdatedRecipes;

  @ConfigEntry.Gui.Tooltip
  @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
  public JournalMode.Type defaultMode;

  @ConfigEntry.Gui.Tooltip
  @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
  public ItemListWidget.Sort defaultNpcSort;

  public GeneralConfig() {
    highlightMissingRecipes = true;
    highlightOutdatedRecipes = true;
    defaultMode = JournalMode.Type.ITEM_SEARCH;
    defaultNpcSort = ItemListWidget.Sort.A_TO_Z;
  }
}
