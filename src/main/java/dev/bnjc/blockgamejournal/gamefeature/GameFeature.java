package dev.bnjc.blockgamejournal.gamefeature;

import dev.bnjc.blockgamejournal.client.BlockgameJournalClient;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;

@Getter
public abstract class GameFeature {
  private MinecraftClient minecraftClient;
  private BlockgameJournalClient blockgameClient;

  public void init(MinecraftClient minecraftClient, BlockgameJournalClient blockgameClient) {
    this.minecraftClient = minecraftClient;
    this.blockgameClient = blockgameClient;
  }

  public void tick(MinecraftClient client) {
  }

  public boolean isEnabled() {
    return true;
  }
}
