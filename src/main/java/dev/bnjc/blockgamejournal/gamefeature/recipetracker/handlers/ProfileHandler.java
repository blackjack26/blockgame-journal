package dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.RecipeTrackerGameFeature;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.listener.screen.ScreenOpenedListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenReceivedInventoryListener;
import dev.bnjc.blockgamejournal.util.NbtUtil;
import dev.bnjc.blockgamejournal.util.Profession;
import dev.bnjc.blockgamejournal.util.StringUtil;
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
  private static final Pattern LEVEL_PATTERN = Pattern.compile("^Level: (\\d+)");
  private static final Pattern IGNORED_PATTERN = Pattern.compile("^(Offense|Defense|Professions|Talents)$");

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

    parseProfessions(packet.getContents());
    return ActionResult.PASS;
  }

  private void parseProfessions(List<ItemStack> inv) {
    // Only look at items in 6x9 menu
    for (int i = 0; i < Math.min(inv.size(), 54); i++) {
      ItemStack itemStack = inv.get(i);
      if (itemStack == null || itemStack.isEmpty()) {
        continue;
      }

      String name = itemStack.getName().getString();
      if (IGNORED_PATTERN.matcher(name).find()) {
        continue;
      }

      Profession profession = Profession.from(name);
      String profName = "";
      if (profession == null) {
        LOGGER.warn("[Blockgame Journal] Unknown profession: {}", name);
        profName = name;
      } else {
        profName = profession.getName();
      }

      NbtList lore = NbtUtil.getLore(itemStack);
      if (lore == null) {
        LOGGER.warn("[Blockgame Journal] No lore found for profession: {}", profName);
        this.setProfessionLevel(profName, null);
        continue;
      }

      Integer foundLevel = null;
      for (int t = 0; t < lore.size(); t++) {
        MutableText textLine = NbtUtil.parseLoreLine(lore.getString(t));
        if (textLine == null) {
          continue;
        }

        Matcher levelMatcher = LEVEL_PATTERN.matcher(StringUtil.removeFormatting(textLine.getString()));
        if (levelMatcher.find()) {
          foundLevel = Integer.parseInt(levelMatcher.group(1));
          break;
        }
      }

      if (foundLevel == null) {
        LOGGER.warn("[Blockgame Journal] No level found for profession: {}", profName);
      }

      this.setProfessionLevel(profName, foundLevel);
    }
  }

  private void setProfessionLevel(String name, @Nullable Integer level) {
    if (Journal.INSTANCE == null) {
      return;
    }

    Journal.instance().getMetadata().setProfessionLevel(name, level);
  }
}
