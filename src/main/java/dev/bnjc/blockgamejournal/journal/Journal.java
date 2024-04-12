package dev.bnjc.blockgamejournal.journal;

import com.mojang.serialization.Codec;
import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.journal.metadata.Metadata;
import dev.bnjc.blockgamejournal.storage.Storage;
import dev.bnjc.blockgamejournal.storage.backend.FileBasedBackend;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Journal {
  private static final Logger LOGGER = BlockgameJournal.getLogger(Journal.class.getCanonicalName());

  public static final Codec<Map<String, ArrayList<JournalEntry>>> JOURNAL_CODEC = Codec
      .unboundedMap(Codec.STRING, Codec.list(JournalEntry.CODEC).xmap(ArrayList::new, Function.identity()))
      .xmap(HashMap::new, Function.identity());

  public static final Codec<Map<String, ItemStack>> KNOWN_ITEMS_CODEC = Codec
      .unboundedMap(Codec.STRING, ItemStack.CODEC)
      .xmap(HashMap::new, Function.identity());

  @Nullable
  public static Journal INSTANCE = null;

  public static void loadDefault() {
    loadOrCreate(Metadata.blankWithName(FileBasedBackend.JOURNAL_NAME));
  }

  public static void loadOrCreate(@NotNull Metadata creationMetadata) {
    unload();
    INSTANCE = Storage.load().orElseGet(() -> new Journal(creationMetadata, new HashMap<>(), new HashMap<>()));
    save();
  }

  public static void save() {
    if (INSTANCE == null) {
      return;
    }

    LOGGER.info("[Blockgame Journal] Saving journal...");
    Storage.save(INSTANCE);
    LOGGER.info("[Blockgame Journal] Journal saved");
  }

  public static void unload() {
    if (INSTANCE == null) {
      return;
    }

    LOGGER.info("[Blockgame Journal] Unloading journal");
    save();
    INSTANCE = null;
  }

  @Getter
  private final Map<String, ArrayList<JournalEntry>> entries;

  @Getter
  private final Map<String, ItemStack> knownItems;

  @Getter
  @Setter
  private Metadata metadata;

  public Journal(Metadata metadata, Map<String, ArrayList<JournalEntry>> entries, Map<String, ItemStack> knownItems) {
    this.metadata = metadata;
    this.entries = entries;
    this.knownItems = knownItems;
  }

  public void addEntry(ItemStack result, JournalEntry entry) {
    String key = ItemUtil.getKey(result);

    LOGGER.info("[Blockgame Journal] Adding entry for item {}", key);
    knownItems.put(key, result); // Add or update the known item

    // If the key does not exist, create a new list with the entry
    if (!entries.containsKey(key)) {
      entries.put(key, new ArrayList<>(List.of(entry)));
      Journal.save();
      return;
    }

    // Otherwise, add the entry to the existing list. But first, check if an entry with the same ingredients already exists
    List<JournalEntry> recipeEntries = entries.get(key);
    JournalEntry existingEntry = recipeEntries
        .stream()
        .filter(e -> e.getIngredients().equals(entry.getIngredients()) && e.getNpcName().equals(entry.getNpcName()))
        .findFirst()
        .orElse(null);
    if (existingEntry != null) {
      LOGGER.warn("[Blockgame Journal] Entry with the same ingredients already exists, overwriting...");
      recipeEntries.set(recipeEntries.indexOf(existingEntry), entry);
      return;
    }

    recipeEntries.add(entry);
    entries.put(key, new ArrayList<>(recipeEntries));
    Journal.save();
  }

  public @Nullable ItemStack getKnownItem(String key) {
    if (key.startsWith("minecraft:")) {
      return new ItemStack(Registries.ITEM.get(new Identifier(key)));
    }
    return knownItems.get(key);
  }

  public boolean hasJournalEntry(ItemStack stack) {
    return this.hasJournalEntry(ItemUtil.getKey(stack));
  }

  public boolean hasJournalEntry(String key) {
    return entries.containsKey(key);
  }

  public @Nullable JournalEntry getFirstJournalEntry(String key) {
    if (!entries.containsKey(key)) {
      return null;
    }

    List<JournalEntry> recipeEntries = entries.get(key);
    return recipeEntries.get(0);
  }

  public boolean removeEntry(String key, int index) {
    if (!entries.containsKey(key)) {
      return false;
    }

    List<JournalEntry> recipeEntries = entries.get(key);
    if (index < 0 || index >= recipeEntries.size()) {
      return false;
    }

    recipeEntries.remove(index);
    if (recipeEntries.isEmpty()) {
      entries.remove(key);
    } else {
      entries.put(key, new ArrayList<>(recipeEntries));
    }
    return true;
  }
}
