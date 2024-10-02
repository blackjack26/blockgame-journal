package dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers.vendor;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.station.CraftingStationItem;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import dev.bnjc.blockgamejournal.journal.metadata.JournalAdvancement;
import dev.bnjc.blockgamejournal.listener.interaction.EntityAttackedListener;
import dev.bnjc.blockgamejournal.listener.interaction.SlotClickedListener;
import dev.bnjc.blockgamejournal.listener.screen.DrawSlotListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenOpenedListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenReceivedInventoryListener;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import dev.bnjc.blockgamejournal.util.NbtUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.KnowledgeBookItem;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VendorHandler {
  private static final Logger LOGGER = BlockgameJournal.getLogger("Vendor Handler");

  /** Used to track the last preview times for the Carpal Tunnel advancement */
  private static final Queue<Long> lastPreviewTimes = new ArrayDeque<>();

  private final List<@Nullable CraftingStationItem> stationItems;
  private RecipeBuilder recipeBuilder;

  private VendorState state = VendorState.INIT;
  private int syncId = -1;

  /**
   * The entity that the player is interacting with. Ideally, this should be the vendor entity.
   */
  @Nullable
  private Entity interactionEntity;
  private String vendorName = "";

  @Nullable
  private CraftingStationItem lastClickedItem;

  private static final byte STATUS_NONE = 0;
  private static final byte STATUS_MISSING = 1;
  private static final byte STATUS_OUTDATED = 1 << 1;
  private static final byte STATUS_LOCKED = 1 << 2;
  private final Map<Integer, Byte> statusCache = new HashMap<>();

  public VendorHandler() {
    this.stationItems = new ArrayList<>();
    this.interactionEntity = null;

    this.recipeBuilder = RecipeBuilder.create();
  }

  public void init() {
    EntityAttackedListener.EVENT.register(this::handleEntityAttacked);
    ScreenOpenedListener.EVENT.register(this::handleOpenScreen);
    ScreenReceivedInventoryListener.EVENT.register(this::handleScreenInventory);
    SlotClickedListener.EVENT.register(this::handleSlotClicked);
    DrawSlotListener.EVENT.register(this::drawSlot);
  }

  private ActionResult handleEntityAttacked(PlayerEntity playerEntity, Entity entity) {
    // New interaction, reset the state
    this.reset();
    this.interactionEntity = entity;

    return ActionResult.PASS;
  }

  private ActionResult handleOpenScreen(OpenScreenS2CPacket packet) {
    String screenName = packet.getName().getString();

    // INIT -> Opened crafting station
    // CRAFTING_STATION -> Refreshed crafting station
    // RECIPE_PREVIEW -> From recipe preview to crafting station
    if (this.state == VendorState.INIT || this.state == VendorState.CRAFTING_STATION || this.state == VendorState.RECIPE_PREVIEW) {
      // If the player is not interacting with any entity, we aren't in a vendor interaction screen
      if (this.interactionEntity != null) {
        String entityName = interactionEntity.getEntityName();

        // Only use custom name if the entity is not a player
        if (!(interactionEntity instanceof PlayerEntity) && interactionEntity.hasCustomName()) {
          entityName = interactionEntity.getCustomName().getString();
        }

        // Look for screen name "Some Name (#page#/#max#)" - exclude "Party" from the name
        Matcher matcher = Pattern.compile("^((?!Party)[^(]+)\\s\\(\\d+/\\d+\\)").matcher(screenName);
        if (matcher.find() || screenName.equals(entityName)) {
          // Citizens2 uses "CIT-<entity name>" as the entity name if no custom name is set
          if (entityName.startsWith("CIT-")) {
            entityName = matcher.group(1);
            interactionEntity.setCustomName(Text.literal(entityName));
          }

          this.syncId = packet.getSyncId();
          this.vendorName = entityName;
          this.stationItems.clear();
          this.statusCache.clear();
          this.state = VendorState.CRAFTING_STATION;
          this.lastClickedItem = null;

          LOGGER.info("[Blockgame Journal] Opened crafting station for {}", this.vendorName);

          return ActionResult.PASS;
        }
      }
    }

    // CRAFTING_STATION -> From crafting station to recipe preview
    // RECIPE_PREVIEW -> Refreshed recipe preview
    // LOADING_RECIPE_PAGE -> Loading the next/previous page
    if (this.state == VendorState.CRAFTING_STATION || this.state == VendorState.RECIPE_PREVIEW || this.state == VendorState.LOADING_RECIPE_PAGE) {
      if (screenName.equals("Recipe Preview")) {
        this.syncId = packet.getSyncId();

        // Reset the recipe page if the screen is not the same as the previous one
        if (this.state != VendorState.LOADING_RECIPE_PAGE) {
          this.recipeBuilder = RecipeBuilder.create();
        }

        this.state = VendorState.RECIPE_PREVIEW;
        return ActionResult.PASS;
      }
    }

    // Not a valid screen, reset the state
    this.reset();
    return ActionResult.PASS;
  }

  private ActionResult handleScreenInventory(InventoryS2CPacket packet) {
    if (packet.getSyncId() != this.syncId) {
      return ActionResult.PASS;
    }

    if (state == VendorState.CRAFTING_STATION) {
      for (ItemStack item : packet.getContents()) {
        if (item == null || item.isEmpty()) {
          this.stationItems.add(null);
          continue;
        }

        CraftingStationItem stationItem = new CraftingStationItem(item, this.stationItems.size());
        NbtList loreTag = NbtUtil.getLore(stationItem.getItem());
        if (loreTag != null) {
          VendorUtil.parseStationItemLore(stationItem, loreTag);
        }

        this.stationItems.add(stationItem);
      }

      VendorUtil.checkForUnavailableItems(this.stationItems, this.vendorName);
    }
    else if (this.state == VendorState.RECIPE_PREVIEW) {
      List<ItemStack> previewItems = packet.getContents();
      boolean hasBackButton = previewItems.get(VendorUtil.BACK_BUTTON_INDEX).getItem() instanceof PlayerHeadItem;
      boolean hasCraftButton = previewItems.get(VendorUtil.CRAFT_BUTTON_INDEX).getItem() instanceof KnowledgeBookItem;
      boolean isScreenValid = hasBackButton && hasCraftButton;

      // Ensure that this is a valid recipe preview screen
      if (!isScreenValid) {
        return ActionResult.PASS;
      }

      this.storeRecipe(previewItems);
    }

    return ActionResult.PASS;
  }

  private ActionResult handleSlotClicked(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player) {
    if (syncId != this.syncId) {
      return ActionResult.PASS;
    }

    if (this.state == VendorState.CRAFTING_STATION) {
      if (slotId < 0 || slotId >= this.stationItems.size()) {
        LOGGER.warn("[Blockgame Journal] Vendor handler slot out of bounds: {}", slotId);
        return ActionResult.PASS;
      }

      CraftingStationItem item = this.stationItems.get(slotId);
      if (item == null || item.getItem().isEmpty()) {
        LOGGER.warn("[Blockgame Journal] Empty item clicked in slot: {}", slotId);
        this.lastClickedItem = null;
        return ActionResult.PASS;
      }

      this.lastClickedItem = item.copy();
    }
    else if (this.state == VendorState.RECIPE_PREVIEW) {
      if (slotId == VendorUtil.PREV_PAGE_BUTTON_INDEX) {
        this.state = VendorState.LOADING_RECIPE_PAGE;
        this.recipeBuilder.goToPreviousPage();
      } else if (slotId == VendorUtil.NEXT_PAGE_BUTTON_INDEX) {
        this.state = VendorState.LOADING_RECIPE_PAGE;
        this.recipeBuilder.goToNextPage();
      }
    }

    return ActionResult.PASS;
  }

  private void drawSlot(DrawContext context, Slot slot) {
    // Verify we are in the correct state
    if (this.syncId == -1 || Journal.INSTANCE == null) {
      return;
    }

    if (this.state == VendorState.CRAFTING_STATION && !this.stationItems.isEmpty()) {
      // Verify the player is still in the same screen
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player != null && player.currentScreenHandler.syncId != this.syncId) {
        this.reset();
        return;
      }

      // Check for configuration settings
      boolean highlightMissing = BlockgameJournal.getConfig().getGeneralConfig().highlightMissingRecipes;
      boolean highlightOutdated = BlockgameJournal.getConfig().getGeneralConfig().highlightOutdatedRecipes;
      boolean showRecipeLock = BlockgameJournal.getConfig().getGeneralConfig().showRecipeLock;
      if (!highlightMissing && !highlightOutdated && !showRecipeLock) {
        return;
      }

      // Check if slot is within bounds (0-53)
      if (slot.id < 0 || slot.id >= this.stationItems.size() || slot.id >= 54) {
        return;
      }

      // Use the cache for drawing so we don't have to recheck every frame
      Byte status = this.statusCache.get(slot.id);
      if (status != null) {
        if ((status & STATUS_MISSING) != 0) {
          VendorUtil.highlightSlot(context, slot, 0x30FF0000);
        } else if ((status & STATUS_OUTDATED) != 0) {
          VendorUtil.highlightSlot(context, slot, 0x40CCCC00);
        }

        if ((status & STATUS_LOCKED) != 0) {
          VendorUtil.drawLocked(context, slot);
        }
        return;
      }

      // See if slot item matches inventory item
      ItemStack slotItem = slot.getStack();
      CraftingStationItem stationItem = this.stationItems.get(slot.id);
      if (slotItem == null || stationItem == null || slotItem.isEmpty()) {
        return;
      }

      if (slotItem.getItem() != stationItem.getItem().getItem()) {
        LOGGER.warn("[Blockgame Journal] Slot item does not match inventory item");
        return;
      }

      boolean recipeNotKnown = Boolean.FALSE.equals(stationItem.getRecipeKnown());
      boolean profRequirementNotMet = false;
      if (stationItem.getRequiredLevel() != -1) {
        int currLevel = Journal.INSTANCE.getMetadata().getProfessionLevels().getOrDefault(stationItem.getRequiredClass(), -1);
        if (currLevel != -1 && currLevel < stationItem.getRequiredLevel()) {
          profRequirementNotMet = true;
        }
      }

      if ((recipeNotKnown || profRequirementNotMet) && showRecipeLock) {
        VendorUtil.drawLocked(context, slot);
        this.statusCache.compute(slot.id, (k, v) -> v == null ? STATUS_LOCKED : (byte) (v | STATUS_LOCKED));
      }

      List<JournalEntry> entries = Journal.INSTANCE.getEntries().getOrDefault(ItemUtil.getKey(stationItem.getItem()), new ArrayList<>());
      for (JournalEntry entry : entries) {
        String expectedNpcName = entry.getNpcName();
        int expectedSlot = entry.getSlot();

        // If the item is in the journal, don't highlight it unless it's outdated
        if (this.vendorName.equals(expectedNpcName) && slot.id == expectedSlot) {
          stationItem.setOutdated(ItemUtil.isOutdated(entry, stationItem));
          if (highlightOutdated && stationItem.getOutdated()) {
            VendorUtil.highlightSlot(context, slot, 0x40CCCC00);
            this.statusCache.compute(slot.id, (k, v) -> v == null ? STATUS_OUTDATED : (byte) (v | STATUS_OUTDATED));
          } else {
            // Negate the status if it's not outdated
            this.statusCache.compute(slot.id, (k, v) -> v == null ? STATUS_NONE : (byte) (v & ~STATUS_OUTDATED));
          }
          return;
        }
      }

      // If the item is not in the journal, highlight it
      if (highlightMissing) {
        VendorUtil.highlightSlot(context, slot, 0x30FF0000);
        this.statusCache.compute(slot.id, (k, v) -> v == null ? STATUS_MISSING : (byte) (v | STATUS_MISSING));
      }
    }
    else if (this.state == VendorState.RECIPE_PREVIEW) {
      // Highlight
      if (slot.id == VendorUtil.NEXT_PAGE_BUTTON_INDEX && !this.recipeBuilder.isStoredRecipe()) {
        ItemStack item = slot.getStack();
        if (item.getItem() instanceof PlayerHeadItem) {
          context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x30FF0000);
          context.drawBorder(slot.x, slot.y, 16, 16, 0xBBFF0000);
          context.getMatrices().push();
          context.getMatrices().translate(0.0f, 0.0f, 200.0f);
          context.drawText(
              MinecraftClient.getInstance().textRenderer,
              "●",
              slot.x + 18 - MinecraftClient.getInstance().textRenderer.getWidth("●"),
              slot.y - 3,
              0xFF3333,
              true
          );
          context.getMatrices().pop();
        }
      }
    }
  }

  private void reset() {
    this.state = VendorState.INIT;
    this.syncId = -1;

    // Entity interaction
    this.lastClickedItem = null;
    this.interactionEntity = null;

    // Crafting station
    this.vendorName = "";
    this.stationItems.clear();
    this.statusCache.clear();

    // Recipe preview
    this.recipeBuilder = RecipeBuilder.create();
  }

  private void storeRecipe(List<ItemStack> previewItems) {
    if (Journal.INSTANCE == null) {
      LOGGER.warn("[Blockgame Journal] Journal is not loaded");
      return;
    }

    if (this.interactionEntity == null) {
      LOGGER.warn("[Blockgame Journal] No station entity to attribute the recipe to");
      return;
    }

    // Validate we came from clicking the item
    if (this.lastClickedItem == null || this.lastClickedItem.getItem().isEmpty()) {
      LOGGER.warn("[Blockgame Journal] No last clicked item found");
      if (this.lastClickedItem != null) {
        LOGGER.warn("[Blockgame Journal] Last clicked item: {}", ItemUtil.getName(this.lastClickedItem.getItem()));
      }
      return;
    }

    boolean hasPrevPageButton = previewItems.get(VendorUtil.PREV_PAGE_BUTTON_INDEX).getItem() instanceof PlayerHeadItem;
    boolean hasNextPageButton = previewItems.get(VendorUtil.NEXT_PAGE_BUTTON_INDEX).getItem() instanceof PlayerHeadItem;

    if (!hasPrevPageButton) {
      this.recipeBuilder.setPage(0);
    }

    ItemStack recipeItem = previewItems.get(VendorUtil.ITEM_INDEX);
    if (recipeItem == null || recipeItem.isEmpty()) {
      LOGGER.warn("[Blockgame Journal] Recipe item is empty, cannot store recipe. Try again later.");
      return;
    }

    this.recipeBuilder.addIngredientsFromPreview(previewItems);

    if (hasNextPageButton) {
      LOGGER.info("[Blockgame Journal] Waiting for next page to store recipe");
      return;
    }

    // Store the recipe in the player's journal
    LOGGER.debug("[Blockgame Journal] Storing recipe for {}", ItemUtil.getName(recipeItem));
    this.recipeBuilder.updateKnownIngredients();
    if (this.recipeBuilder.createEntry(recipeItem, this.interactionEntity, this.lastClickedItem, this.vendorName)) {
      this.checkForAdvancement();
    }
  }

  private void checkForAdvancement() {
    if (Journal.INSTANCE == null || Journal.INSTANCE.getMetadata().hasAdvancement(JournalAdvancement.CARPAL_TUNNEL)) {
      return;
    }

    lastPreviewTimes.add(System.currentTimeMillis());

    if (lastPreviewTimes.size() >= 25) {
      long firstTime = lastPreviewTimes.poll();

      // If it has been less than a minute since the first preview, grant the advancement
      if (System.currentTimeMillis() - firstTime <= 60000) {
        JournalAdvancement.CARPAL_TUNNEL.grant();
      }
    }
  }

  enum VendorState {
    INIT,
    CRAFTING_STATION,
    RECIPE_PREVIEW,
    LOADING_RECIPE_PAGE,
  }
}
