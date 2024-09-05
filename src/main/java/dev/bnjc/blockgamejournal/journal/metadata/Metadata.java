package dev.bnjc.blockgamejournal.journal.metadata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bnjc.blockgamejournal.BlockgameJournal;
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
          }),
          Codec.unboundedMap(Codec.STRING, Codec.INT)
              .xmap(HashMap::new, Function.identity())
              .optionalFieldOf("backpackContents")
              .forGetter(meta -> Optional.ofNullable(meta.backpackContents)),
          Codec.LONG.optionalFieldOf("backpackLastUpdated").forGetter(meta -> {
            if (meta.backpackLastUpdated == null) {
              return Optional.empty();
            }
            return Optional.of(meta.backpackLastUpdated.toEpochMilli());
          }),
          Codec.unboundedMap(Codec.STRING, Codec.BOOL)
              .xmap(HashMap::new, Function.identity())
              .optionalFieldOf("knownRecipes")
              .forGetter(meta -> Optional.ofNullable(meta.knownRecipes)),
          Codec.unboundedMap(JournalAdvancement.CODEC, Codec.BOOL)
              .xmap(HashMap::new, Function.identity())
              .optionalFieldOf("advancements")
              .forGetter(meta -> Optional.ofNullable(meta.advancements)),
          Codec.unboundedMap(Codec.STRING, Codec.INT)
              .xmap(HashMap::new, Function.identity())
              .optionalFieldOf("manuallyCompletedTracking")
              .forGetter(meta -> Optional.ofNullable(meta.manuallyCompletedTracking))
      ).apply(instance, (name, lastModified, loadedTime, playerBalance, balanceLastUpdated,
                         professionLevels, professionLastUpdated,
                         backpackContents, backpackLastUpdated,
                         knownRecipes, advancements, manuallyCompletedTracking) -> {
        Metadata meta = new Metadata(
            name.orElse(null),
            lastModified.map(Instant::ofEpochMilli).orElse(Instant.now()),
            loadedTime
        );

        playerBalance.ifPresent(meta::setPlayerBalance);
        balanceLastUpdated.ifPresent(time -> meta.balanceLastUpdated = Instant.ofEpochMilli(time));

        professionLevels.ifPresent(meta::setProfessionLevels);
        professionLastUpdated.ifPresent(time -> meta.professionsLastUpdated = Instant.ofEpochMilli(time));

        backpackContents.ifPresent(meta::setBackpackContents);
        backpackLastUpdated.ifPresent(time -> meta.backpackLastUpdated = Instant.ofEpochMilli(time));

        knownRecipes.ifPresent(meta::setKnownRecipes);
        advancements.ifPresent(meta::setAdvancements);
        manuallyCompletedTracking.ifPresent(meta::setManuallyCompletedTracking);

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

  @Setter
  private HashMap<String, Integer> backpackContents;
  @Setter
  private Instant backpackLastUpdated;

  @Setter
  private HashMap<String, Boolean> knownRecipes;

  @Setter
  private HashMap<JournalAdvancement, Boolean> advancements;

  @Setter
  private HashMap<String, Integer> manuallyCompletedTracking;

  public Metadata(@Nullable String name, Instant lastModified, long loadedTime) {
    this.name = name;
    this.lastModified = lastModified;
    this.loadedTime = loadedTime;
    this.playerBalance = -1f;
    this.balanceLastUpdated = null;
    this.professionLevels = new HashMap<>();
    this.professionsLastUpdated = null;
    this.backpackContents = new HashMap<>();
    this.backpackLastUpdated = null;
    this.knownRecipes = new HashMap<>();
    this.advancements = new HashMap<>();
    this.manuallyCompletedTracking = new HashMap<>();
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

  public void setBackpack(Map<String, Integer> backpackContents) {
    this.backpackContents = new HashMap<>(backpackContents);
    this.backpackLastUpdated = Instant.now();
  }

  public void setKnownRecipe(String recipe, boolean known) {
    this.knownRecipes.put(recipe, known);
  }

  public Boolean getKnownRecipe(String recipe) {
    return this.knownRecipes.get(recipe);
  }

  public void grantAdvancement(JournalAdvancement advancement) {
    this.advancements.put(advancement, true);
  }

  public boolean hasAdvancement(JournalAdvancement advancement) {
    return this.advancements.getOrDefault(advancement, false);
  }

  public int getManualCount(String key) {
    if (!BlockgameJournal.getConfig().getGeneralConfig().enableManualTracking) {
      return 0;
    }

    return this.manuallyCompletedTracking.getOrDefault(key, 0);
  }

  public void removeManualCount(String key) {
    if (!BlockgameJournal.getConfig().getGeneralConfig().enableManualTracking) {
      return;
    }

    this.manuallyCompletedTracking.remove(key);
  }

  public void adjustManualCount(String key, int amount) {
    if (!BlockgameJournal.getConfig().getGeneralConfig().enableManualTracking) {
      return;
    }

    this.manuallyCompletedTracking.put(key, Math.max(this.getManualCount(key) + amount, 0));
  }
}
