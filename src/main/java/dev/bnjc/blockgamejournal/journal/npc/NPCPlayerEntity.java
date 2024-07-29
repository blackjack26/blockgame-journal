package dev.bnjc.blockgamejournal.journal.npc;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;

public class NPCPlayerEntity extends AbstractClientPlayerEntity {
  private PlayerListEntry playerListEntry;

  public NPCPlayerEntity(ClientWorld world, GameProfile profile) {
    super(world, profile);

    // Set the player model parts to all visible
    this.getDataTracker().set(PLAYER_MODEL_PARTS, (byte) 0x7F);
  }

  @Nullable
  @Override
  protected PlayerListEntry getPlayerListEntry() {
    if (this.playerListEntry == null) {
      GameProfile gameProfile = this.getGameProfile();
      this.playerListEntry = new PlayerListEntry(gameProfile, false);
    }

    return this.playerListEntry;
  }
}
