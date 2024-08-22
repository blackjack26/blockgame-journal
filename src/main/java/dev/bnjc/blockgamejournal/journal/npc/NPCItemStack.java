package dev.bnjc.blockgamejournal.journal.npc;

import com.mojang.authlib.GameProfile;
import dev.bnjc.blockgamejournal.journal.Journal;
import lombok.Getter;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.html.Option;
import java.util.Optional;

@Getter
public class NPCItemStack {
  public static final String NPC_NAME_KEY = "BGJ_NPC";

  private final String npcName;
  private final NPCEntry npcEntry;
  private final ItemStack itemStack;

  private NPCItemStack(String npcName, NPCEntry npcEntry, ItemStack itemStack) {
    this.npcName = npcName;
    this.npcEntry = npcEntry;
    this.itemStack = itemStack;

    this.updateItemStack();
  }

  public static Optional<NPCItemStack> from(@NotNull String npcName) {
    if (Journal.INSTANCE == null) {
      return Optional.empty();
    }

    Optional<NPCEntry> maybeEntry = Journal.INSTANCE.getKnownNpc(npcName);
    if (maybeEntry.isEmpty()) {
      return Optional.empty();
    }

    NPCEntry entry = maybeEntry.get();
    ItemStack stack;

    if (entry.getEntityType().equals("PlayerEntity") || // Legacy
        entry.getEntityType().equals(EntityType.getId(EntityType.PLAYER).toString())) {
      stack = new ItemStack(Items.PLAYER_HEAD);
    } else if (entry.getEntityType().equals("ChickenEntity")) { // Legacy
      stack = new ItemStack(Items.CHICKEN_SPAWN_EGG);
    } else {
      // Create a spawn egg for the entity type
      Optional<EntityType<?>> entityType = EntityType.get(entry.getEntityType());
      if (entityType.isEmpty()) {
        return Optional.empty();
      }

      SpawnEggItem eggItem = SpawnEggItem.forEntity(entityType.get());
      if (eggItem == null) {
        return Optional.empty();
      }

      stack = new ItemStack(eggItem);
    }

    return Optional.of(new NPCItemStack(npcName, entry, stack));
  }

  public static void updateStack(String npcName, ItemStack stack, NPCEntry entry) {
    new NPCItemStack(npcName, entry, stack);
  }

  public void updateItemStack() {
    // Set the skull owner to the NPC's game profile
    GameProfile gameProfile = this.npcEntry.getGameProfile();
    this.itemStack.setSubNbt(PlayerHeadItem.SKULL_OWNER_KEY, NbtHelper.writeGameProfile(new NbtCompound(), gameProfile));

    // Set NPC name to the NBT
    this.itemStack.setSubNbt(NPC_NAME_KEY, NbtString.of(npcName));

    this.populateTooltip();
  }

  private void populateTooltip() {
    // Update the stack's custom name
    NPCNames.NPCName npcNameObj = NPCNames.get(npcName);
    MutableText npcNameText = Text.literal(npcNameObj.name());
    npcNameText.setStyle(npcNameText.getStyle().withItalic(false).withFormatting(Formatting.WHITE));
    this.itemStack.setCustomName(npcNameText);

    // Populate lore
    NbtList loreNbt = new NbtList();

    // Set "Lore" to the title of the NPC
    if (npcNameObj.title() != null) {
      MutableText loreText = Text.literal("« " + npcNameObj.title() + " »");
      loreText.setStyle(loreText.getStyle().withItalic(false).withFormatting(Formatting.GRAY));
      loreNbt.add(NbtString.of(Text.Serializer.toJson(loreText)));
    }

    loreNbt.add(NbtString.of(Text.Serializer.toJson(Text.literal(""))));
    if (npcEntry.isLocating()) {
      // Display helper text in the lore:
      // "Locating NPC in the world..."
      MutableText locatingText = Text.literal(" Locating NPC...");
      locatingText.setStyle(locatingText.getStyle().withItalic(true).withFormatting(Formatting.DARK_PURPLE));
      loreNbt.add(NbtString.of(Text.Serializer.toJson(locatingText)));

      // "Press A to stop locating" (color "Press A" in green, the rest in gray)
      MutableText stopLocatingText = Text.literal(" ⚐ ").formatted(Formatting.GRAY)
          .append(Text.literal("Press A").formatted(Formatting.RED))
          .append(Text.literal(" to stop locating").formatted(Formatting.GRAY));
      stopLocatingText.setStyle(stopLocatingText.getStyle().withItalic(false));
      loreNbt.add(NbtString.of(Text.Serializer.toJson(stopLocatingText)));
    }
    else if (npcEntry.getPosition() != null) {
      // Display helper text in the lore:
      // "Press A to locate NPC in the world"
      MutableText locateText = Text.literal(" ⚐ ").formatted(Formatting.GRAY)
          .append(Text.literal("Press A").formatted(Formatting.GREEN))
          .append(Text.literal(" to locate").formatted(Formatting.GRAY));
      locateText.setStyle(locateText.getStyle().withItalic(false));
      loreNbt.add(NbtString.of(Text.Serializer.toJson(locateText)));
    }

    MutableText removeNpcText = Text.literal(" ⚐ ").formatted(Formatting.GRAY)
        .append(Text.literal("Press X").formatted(Formatting.RED))
        .append(Text.literal(" to remove NPC").formatted(Formatting.GRAY));
    removeNpcText.setStyle(removeNpcText.getStyle().withItalic(false));
    loreNbt.add(NbtString.of(Text.Serializer.toJson(removeNpcText)));

    this.itemStack.getOrCreateSubNbt(ItemStack.DISPLAY_KEY).put(ItemStack.LORE_KEY, loreNbt);
  }
}
