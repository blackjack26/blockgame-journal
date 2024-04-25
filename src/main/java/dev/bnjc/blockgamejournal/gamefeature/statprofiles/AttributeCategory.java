package dev.bnjc.blockgamejournal.gamefeature.statprofiles;

public enum AttributeCategory {
  OFFENSE,
  SUPPORT,
  DEFENSE,
  OTHER;

  public boolean isOffense() {
    return this == OFFENSE;
  }

  public boolean isSupport() {
    return this == SUPPORT;
  }

  public boolean isDefense() {
    return this == DEFENSE;
  }

  public boolean isOther() {
    return this == OTHER;
  }
}
