package dev.bnjc.blockgamejournal.journal.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@Getter
public class NPCEntry {
  public static final Codec<NPCEntry> CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.STRING.fieldOf("name").forGetter(NPCEntry::getName),
          NbtCompound.CODEC.xmap(
              NbtHelper::toGameProfile,
              gameProfile -> NbtHelper.writeGameProfile(new NbtCompound(), gameProfile)
          ).fieldOf("gameProfile").forGetter(NPCEntry::getGameProfile),
          BlockPos.CODEC.optionalFieldOf("position").forGetter(entry -> {
            if (entry.getPosition() == null) {
              return Optional.empty();
            }
            return Optional.of(entry.getPosition());
          }),
          Codec.STRING.optionalFieldOf("world").forGetter(entry -> {
            if (entry.getWorld() == null) {
              return Optional.empty();
            }
            return Optional.of(entry.getWorld());
          }),
          Codec.STRING.optionalFieldOf("className").forGetter(entry -> {
            if (entry.getClassName() == null) {
              return Optional.empty();
            }
            return Optional.of(entry.getClassName());
          })
      ).apply(instance, (name, gameProfile, position, world, className) -> {
        NPCEntry entry = new NPCEntry(name, gameProfile, position.orElse(null), world.orElse(null));
        entry.className = className.orElse("PlayerEntity");
        return entry;
      })
  );

  private final String name;
  private final GameProfile gameProfile;
  private final @Nullable BlockPos position;
  private final @Nullable String world;
  private String className;

  @Setter
  private boolean locating;

  public NPCEntry(String name, GameProfile gameProfile, @Nullable BlockPos position, @Nullable String world) {
    this.name = name;
    this.gameProfile = gameProfile;
    this.position = position;
    this.world = world;

    this.locating = false;
    this.className = "PlayerEntity";
  }

  public static NPCEntry of(Entity entity) {
    String world = entity.getEntityWorld().getRegistryKey().getValue().toString();
    if (entity instanceof PlayerEntity) {
      return new NPCEntry(entity.getEntityName(), ((PlayerEntity)entity).getGameProfile(), entity.getBlockPos(), world);
    }

    // Create a new GameProfile with the entity's UUID and name
    String name = entity.getEntityName();
    if (entity.hasCustomName()) {
      name = entity.getCustomName().getString();
    }

    GameProfile profile = new GameProfile(entity.getUuid(), name);
    NPCEntry entry = new NPCEntry(name, profile, entity.getBlockPos(), world);

    if (entity instanceof ChickenEntity) {
      entry.className = "ChickenEntity";
    } else {
      entry.className = "Unknown::" + entity.getClass().getSimpleName();
    }
    return entry;
  }

  public static NPCEntry fromLegacy(String name, GameProfile profile) {
    return new NPCEntry(name, profile, null, null);
  }

  public int getX() {
    return (int) (position.getX() / MinecraftClient.getInstance().world.getDimension().coordinateScale());
  }

  public int getY() {
    return this.position.getY();
  }

  public int getZ() {
    return (int) (position.getZ() / MinecraftClient.getInstance().world.getDimension().coordinateScale());
  }

  public int compareTo(NPCEntry other) {
    double myDistance = this.getDistanceSqToEntity(MinecraftClient.getInstance().player);
    double otherDistance = other.getDistanceSqToEntity(MinecraftClient.getInstance().player);
    return Double.compare(myDistance, otherDistance);
  }

  public double getDistanceSqToEntity(Entity entity) {
    return entity.squaredDistanceTo(this.getX() + 0.5, this.getY() + 0.5, this.getZ() + 0.5);
  }

  public UUID getId() {
    return this.gameProfile.getId();
  }
}
