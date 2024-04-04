package dev.bnjc.blockgamejournal.journal.metadata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;

@Getter
public class Metadata {
  public static final Codec<Metadata> CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
          Codec.STRING.optionalFieldOf("name").forGetter(meta -> Optional.ofNullable(meta.name)),
          Codec.LONG.optionalFieldOf("lastModified").forGetter(meta -> Optional.of(meta.lastModified.toEpochMilli())),
          Codec.LONG.fieldOf("loadedTime").forGetter(Metadata::getLoadedTime)
      ).apply(instance, (name, lastModified, loadedTime) -> new Metadata(
          name.orElse(null),
          lastModified.map(Instant::ofEpochMilli).orElse(Instant.now()),
          loadedTime
      ))
  );

  @Nullable
  private String name;
  private Instant lastModified;
  private long loadedTime;

  public Metadata(@Nullable String name, Instant lastModified, long loadedTime) {
    this.name = name;
    this.lastModified = lastModified;
    this.loadedTime = loadedTime;
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
}
