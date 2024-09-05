package dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers.vendor;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.station.CraftingStationItem;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import dev.bnjc.blockgamejournal.util.NbtUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VendorUtil {
  public static final int BACK_BUTTON_INDEX = 10;
  public static final int ITEM_INDEX = 16;
  public static final int PREV_PAGE_BUTTON_INDEX = 20;
  public static final int NEXT_PAGE_BUTTON_INDEX = 24;
  public static final int CRAFT_BUTTON_INDEX = 28;
  public static final int CONFIRM_BUTTON_INDEX = 34;

  private static final Logger LOGGER = BlockgameJournal.getLogger("Vendor Util");

  private static final Pattern COIN_PATTERN = Pattern.compile("([✔✖]) Requires (\\d+(?:\\.\\d+)?) Coin");
  private static final Pattern KNOWLEDGE_PATTERN = Pattern.compile("([✔✖]) Recipe (Known|Learned)");
  private static final Pattern CLASS_PATTERN = Pattern.compile("([✔✖]) Requires (\\d+) in ([A-Za-z]+)");
  private static final Pattern INGREDIENT_HEADER_PATTERN = Pattern.compile("Ingredients:");
  private static final Pattern INGREDIENT_PATTERN = Pattern.compile("([✔✖]) (\\d+) (.+?)$");

  private VendorUtil() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * Parses the metadata from the clicked item's lore. This includes the coin cost, recipe known status, class
   * requirement, and expected ingredients.
   */
  public static void parseStationItemLore(@Nullable CraftingStationItem item, NbtList loreTag) {
    if (item == null) {
      LOGGER.warn("[Blockgame Journal] No item found for lore");
      return;
    }

    if (loreTag == null) {
      LOGGER.warn("[Blockgame Journal] No lore tag found");
      return;
    }

    boolean listingIngredients = false;
    for (int i = 0; i < loreTag.size(); i++) {
      String lore = loreTag.getString(i);
      MutableText text = NbtUtil.parseLoreLine(lore);
      if (text == null) {
        LOGGER.warn("[Blockgame Journal] Failed to parse lore line: {}", lore);
        continue;
      }

      lore = text.getString();

      if (listingIngredients) {
        Matcher ingredientMatcher = INGREDIENT_PATTERN.matcher(lore);
        if (ingredientMatcher.find()) {
          String name = ingredientMatcher.group(3);
          item.addExpectedIngredient(name, Integer.parseInt(ingredientMatcher.group(2)));
          continue;
        }

        if (lore.isBlank()) {
          // If we find an empty text, we might have more ingredients on the next line
          continue;
        }

        // If we didn't match an ingredient, we're done listing them
        listingIngredients = false;
      } else {
        // Check for coin cost
        Matcher coinMatcher = COIN_PATTERN.matcher(lore);
        if (coinMatcher.find()) {
          item.setCost(Float.parseFloat(coinMatcher.group(2)));
          continue;
        }

        // Check for recipe known
        Matcher knowledgeMatcher = KNOWLEDGE_PATTERN.matcher(lore);
        if (knowledgeMatcher.find()) {
          item.setRecipeKnown("✔".equals(knowledgeMatcher.group(1)));
          continue;
        }

        // Check for class requirement
        Matcher classMatcher = CLASS_PATTERN.matcher(lore);
        if (classMatcher.find()) {
          item.setRequiredLevel(Integer.parseInt(classMatcher.group(2)));
          item.setRequiredClass(classMatcher.group(3));
          continue;
        }

        // Check for ingredients header
        Matcher ingredientMatcher = INGREDIENT_HEADER_PATTERN.matcher(lore);
        if (ingredientMatcher.find()) {
          listingIngredients = true;
          continue;
        }
      }

      LOGGER.debug("[Blockgame Journal] Unrecognized lore: {}", lore);
    }
  }

  public static void checkForUnavailableItems(List<CraftingStationItem> stationItems, String vendorName) {
    if (Journal.INSTANCE == null) {
      return;
    }

    for (JournalEntry journalEntry : Journal.INSTANCE.getEntriesForVendor(vendorName)) {
      boolean found = false;

      // See if there is a matching item in the inventory in the same slot
      for (CraftingStationItem stationItem : stationItems) {
        if (stationItem == null) {
          continue;
        }

        if (ItemUtil.getKey(stationItem.getItem()).equals(journalEntry.getKey())) {
          found = true;
          break;
        }
      }

      journalEntry.setUnavailable(!found);
    }
  }

  public static void highlightSlot(DrawContext context, Slot slot, int color) {
    context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
    context.drawBorder(slot.x, slot.y,  16, 16, color | 0xBB000000);
  }

  public static void drawLocked(DrawContext context, Slot slot) {
    context.getMatrices().push();
    context.getMatrices().translate(0, 0, 150);
    context.drawGuiTexture(GuiUtil.sprite("lock_icon"), slot.x + 10, slot.y - 2, 150, 8, 8);
    context.getMatrices().pop();
  }
}
