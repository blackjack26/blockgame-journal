package dev.bnjc.blockgamejournal.journal.npc;

import dev.bnjc.blockgamejournal.journal.Journal;
import lombok.Getter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;

public class NPCEntity {
  @Getter
  private final NPCNames.NPCName npcName;

  /**
   * The name of the NPC in the world. This is used to look up the NPC in the journal.
   */
  @Getter
  private final String npcWorldName;

  @Getter
  private final NPCEntry npcEntry;

  @Getter
  private final LivingEntity entity;

  public NPCEntity(ClientWorld world, String npcWorldName) {
    this.npcEntry = Journal.INSTANCE.getKnownNPCs().get(npcWorldName);
    this.npcName = NPCNames.get(npcWorldName);
    this.npcWorldName = npcWorldName;

    if (this.npcEntry.getClassName().equals("PlayerEntity")) {
      this.entity = new NPCPlayerEntity(world, Journal.INSTANCE.getKnownNPCs().get(npcWorldName).getGameProfile());
    }
    else if (this.npcEntry.getClassName().equals("ChickenEntity")) {
      this.entity = new ChickenEntity(EntityType.CHICKEN, world);
    }
    else {
      this.entity = null;
    }
  }
}
