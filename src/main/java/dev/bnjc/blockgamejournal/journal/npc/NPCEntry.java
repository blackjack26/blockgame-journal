package dev.bnjc.blockgamejournal.journal.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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
          })
      ).apply(instance, (name, gameProfile, position, world) ->
          new NPCEntry(name, gameProfile, position.orElse(null), world.orElse(null))
      )
  );

  private final String name;
  private final GameProfile gameProfile;
  private final @Nullable BlockPos position;
  private final @Nullable String world;

  @Setter
  private boolean locating;

  public NPCEntry(String name, GameProfile gameProfile, @Nullable BlockPos position, @Nullable String world) {
    this.name = name;
    this.gameProfile = gameProfile;
    this.position = position;
    this.world = world;

    this.locating = false;
  }

  public static NPCEntry of(PlayerEntity entity) {
    String world = entity.getEntityWorld().getRegistryKey().getValue().toString();
    return new NPCEntry(entity.getEntityName(), entity.getGameProfile(), entity.getBlockPos(), world);
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
}
