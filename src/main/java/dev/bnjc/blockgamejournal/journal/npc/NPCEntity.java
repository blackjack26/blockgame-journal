package dev.bnjc.blockgamejournal.journal.npc;

import com.mojang.authlib.GameProfile;
import dev.bnjc.blockgamejournal.journal.Journal;
import lombok.Getter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;

public class NPCEntity extends AbstractClientPlayerEntity {
  @Getter
  private final NPCNames.NPCName npcName;

  /**
   * The name of the NPC in the world. This is used to look up the NPC in the journal.
   */
  @Getter
  private final String npcWorldName;
  private PlayerListEntry playerListEntry;

  public NPCEntity(ClientWorld world, String npcWorldName) {
    super(world, Journal.INSTANCE.getKnownNPCs().get(npcWorldName));

    this.npcName = NPCNames.get(npcWorldName);
    this.npcWorldName = npcWorldName;

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
