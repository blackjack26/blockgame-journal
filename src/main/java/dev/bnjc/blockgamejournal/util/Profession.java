package dev.bnjc.blockgamejournal.util;

import lombok.Getter;

@Getter
public enum Profession {
  MINING("Mining", 10),
  LOGGING("Logging", 11),
  ARCHAEOLOGY("Archaeology", 12),
  EINHERJAR("Profile", 15),
  FISHING("Fishing", 19),
  HERBALISM("Herbalism", 20),
  RUNECARVING("Runecarving", 21);

  private final String name;
  private final int slot;

  Profession(String name, int slot) {
    this.name = name;
    this.slot = slot;
  }


}
