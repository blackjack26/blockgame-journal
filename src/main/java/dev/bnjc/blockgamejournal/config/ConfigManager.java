package dev.bnjc.blockgamejournal.config;

import dev.bnjc.blockgamejournal.storage.Storage;
import lombok.Getter;

@Getter
public class ConfigManager {

  private final Storage storage;

  public ConfigManager() {
    storage = new Storage();
  }

}
