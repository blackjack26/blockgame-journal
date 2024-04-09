package dev.bnjc.blockgamejournal.journal.metadata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Getter
public class Metadata {
  public static final Codec<Metadata> CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.STRING.optionalFieldOf("name").forGetter(meta -> Optional.ofNullable(meta.name)),
          Codec.LONG.optionalFieldOf("lastModified").forGetter(meta -> Optional.of(meta.lastModified.toEpochMilli())),
          Codec.LONG.fieldOf("loadedTime").forGetter(Metadata::getLoadedTime),
          Codec.FLOAT.optionalFieldOf("playerBalance").forGetter(meta -> Optional.of(meta.playerBalance)),
          Codec.LONG.optionalFieldOf("balanceLastUpdated").forGetter(meta -> {
            if (meta.balanceLastUpdated == null) {
              return Optional.empty();
            }
            return Optional.of(meta.balanceLastUpdated.toEpochMilli());
          }),
          Codec.unboundedMap(Codec.STRING, Codec.INT)
              .xmap(HashMap::new, Function.identity())
              .optionalFieldOf("professionLevels")
              .forGetter(meta -> Optional.of(meta.professionLevels)),
          Codec.LONG.optionalFieldOf("professionsLastUpdated").forGetter(meta -> {
            if (meta.professionsLastUpdated == null) {
              return Optional.empty();
            }
            return Optional.of(meta.professionsLastUpdated.toEpochMilli());
          })
      ).apply(instance, (name, lastModified, loadedTime, playerBalance, balanceLastUpdated, professionLevels, professionLastUpdated) -> {
        Metadata meta = new Metadata(
            name.orElse(null),
            lastModified.map(Instant::ofEpochMilli).orElse(Instant.now()),
            loadedTime
        );

        playerBalance.ifPresent(meta::setPlayerBalance);
        balanceLastUpdated.ifPresent(time -> meta.balanceLastUpdated = Instant.ofEpochMilli(time));

        professionLevels.ifPresent(meta::setProfessionLevels);
        professionLastUpdated.ifPresent(time -> meta.professionsLastUpdated = Instant.ofEpochMilli(time));

        return meta;
      })
  );

  @Nullable
  private String name;
  private Instant lastModified;
  private long loadedTime;

  private float playerBalance;
  @Setter
  private Instant balanceLastUpdated;

  @Setter
  private HashMap<String, Integer> professionLevels;
  @Setter
  private Instant professionsLastUpdated;

  public Metadata(@Nullable String name, Instant lastModified, long loadedTime) {
    this.name = name;
    this.lastModified = lastModified;
    this.loadedTime = loadedTime;
    this.playerBalance = -1f;
    this.balanceLastUpdated = null;
    this.professionLevels = new HashMap<>();
    this.professionsLastUpdated = null;
  }

  public static Metadata blank() {
    return new Metadata(null, Instant.now(), 0L);
  }

  public static Metadata blankWithName(String name) {
    return new Metadata(name, Instant.now(), 0L);
  }

  public void updateLastModified() {
    this.lastModified = Instant.now();
  }

  public Metadata deepCopy() {
    return new Metadata(this.name, this.lastModified, this.loadedTime);
  }

  public void incrementLoadedTime() {
    this.loadedTime++;
  }

  public void setPlayerBalance(float playerBalance) {
    this.playerBalance = playerBalance;

    if (this.playerBalance == -1f) {
      this.balanceLastUpdated = null;
    } else {
      this.balanceLastUpdated = Instant.now();
    }
  }

  public void setProfessionLevel(String profession, Integer level) {
    if (level == null) {
      this.professionLevels.remove(profession);
    } else {
      this.professionLevels.put(profession, level);
    }

    this.professionsLastUpdated = Instant.now();
  }
}
