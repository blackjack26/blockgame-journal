package dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.RecipeTrackerGameFeature;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.listener.screen.ScreenOpenedListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenReceivedInventoryListener;
import dev.bnjc.blockgamejournal.util.NbtUtil;
import dev.bnjc.blockgamejournal.util.Profession;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileHandler {
  private static final Logger LOGGER = BlockgameJournal.getLogger("Profile Handler");
  private static final Pattern LEVEL_PATTERN = Pattern.compile("Level: (\\d+)");

  private final RecipeTrackerGameFeature gameFeature;
  private int syncId = -1;

  public ProfileHandler(RecipeTrackerGameFeature gameFeature) {
    this.gameFeature = gameFeature;
  }

  public void init() {
    ScreenOpenedListener.EVENT.register(this::handleOpenScreen);
    ScreenReceivedInventoryListener.EVENT.register(this::handleScreenInventory);
  }

  private ActionResult handleOpenScreen(OpenScreenS2CPacket packet) {
    String screenName = packet.getName().getString();
    if (screenName.equals("Your Character")) {
      this.syncId = packet.getSyncId();
    } else {
      this.syncId = -1;
    }

    return ActionResult.PASS;
  }

  private ActionResult handleScreenInventory(InventoryS2CPacket packet) {
    if (packet.getSyncId() != this.syncId) {
      return ActionResult.PASS;
    }

    List<ItemStack> inv = packet.getContents();
    parseProfessionLevel(inv, Profession.MINING);
    parseProfessionLevel(inv, Profession.LOGGING);
    parseProfessionLevel(inv, Profession.ARCHAEOLOGY);
    parseProfessionLevel(inv, Profession.EINHERJAR);
    parseProfessionLevel(inv, Profession.FISHING);
    parseProfessionLevel(inv, Profession.HERBALISM);
    parseProfessionLevel(inv, Profession.RUNECARVING);

    return ActionResult.PASS;
  }

  private void parseProfessionLevel(List<ItemStack> inv, Profession profession) {
    int slot = profession.getSlot();
    ItemStack stack = inv.get(slot);
    if (stack.isEmpty()) {
      LOGGER.warn("[Blockgame Journal] Empty slot: {}", slot);
      this.setProfessionLevel(profession, null);
      return;
    }

    NbtList lore = NbtUtil.getLore(stack);
    if (lore == null) {
      LOGGER.warn("[Blockgame Journal] No lore found in slot: {}", slot);
      this.setProfessionLevel(profession, null);
      return;
    }

    for (int i = 0; i < lore.size(); i++) {
      MutableText textLine = NbtUtil.parseLoreLine(lore.getString(i));
      if (textLine == null) {
        continue;
      }

      String line = textLine.getString();
      if (line.contains(profession == Profession.EINHERJAR ? "Level:" : "Current Level:")) {
        Matcher levelMatcher = LEVEL_PATTERN.matcher(line);
        if (levelMatcher.find()) {
          this.setProfessionLevel(profession, Integer.parseInt(levelMatcher.group(1)));
          return;
        }
      }
    }

    LOGGER.warn("[Blockgame Journal] No level found in lore");
    this.setProfessionLevel(profession, null);
  }

  private void setProfessionLevel(Profession profession, @Nullable Integer level) {
    if (Journal.INSTANCE == null) {
      return;
    }

    Journal.INSTANCE.getMetadata().setProfessionLevel(profession.getName(), level);
  }
}
