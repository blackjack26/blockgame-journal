package dev.bnjc.blockgamejournal.journal.npc;

import dev.bnjc.blockgamejournal.journal.Journal;
import lombok.Getter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;

import java.util.Optional;

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

    if (this.npcEntry.getEntityType().equals("PlayerEntity") || // Legacy
        this.npcEntry.getEntityType().equals(EntityType.getId(EntityType.PLAYER).toString())) {
      this.entity = new NPCPlayerEntity(world, Journal.INSTANCE.getKnownNPCs().get(npcWorldName).getGameProfile());
    }
    else if (this.npcEntry.getEntityType().equals("ChickenEntity")) {
      this.entity = EntityType.CHICKEN.create(world);
    }
    else {
      Optional<EntityType<?>> maybeEntity = EntityType.get(this.npcEntry.getEntityType());
      if (maybeEntity.isPresent()) {
        Entity e = maybeEntity.get().create(world);
        if (e instanceof LivingEntity) {
          this.entity = (LivingEntity) e;
        }
        else {
          this.entity = null;
        }
      }
      else {
        this.entity = null;
      }
    }
  }
}
