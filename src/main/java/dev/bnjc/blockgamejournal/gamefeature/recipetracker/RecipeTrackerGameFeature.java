package dev.bnjc.blockgamejournal.gamefeature.recipetracker;

import com.mojang.brigadier.CommandDispatcher;
import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.client.BlockgameJournalClient;
import dev.bnjc.blockgamejournal.gamefeature.GameFeature;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers.CraftingStationHandler;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers.RecipePreviewHandler;
import dev.bnjc.blockgamejournal.gui.screen.RecipeJournalScreen;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.listener.interaction.EntityAttackedListener;
import dev.bnjc.blockgamejournal.listener.interaction.SlotClickedListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenOpenedListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenReceivedInventoryListener;
import dev.bnjc.blockgamejournal.storage.Storage;
import lombok.Getter;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class RecipeTrackerGameFeature extends GameFeature {
  private static final Logger LOGGER = BlockgameJournal.getLogger("Recipe Tracker");

  private static final KeyBinding OPEN_GUI = KeyBindingHelper.registerKeyBinding(
      new KeyBinding("key.blockgamejournal.open_gui", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_J, "category.blockgamejournal")
  );

  @Getter
  private final RecipePreviewHandler recipePreviewHandler;

  @Getter
  private final CraftingStationHandler craftingStationHandler;


  @Getter
  @Nullable
  private PlayerEntity lastAttackedPlayer = null;

  public RecipeTrackerGameFeature() {
    this.recipePreviewHandler = new RecipePreviewHandler(this);
    this.craftingStationHandler = new CraftingStationHandler(this);
  }

  @Override
  public void init(MinecraftClient minecraftClient, BlockgameJournalClient blockgameClient) {
    super.init(minecraftClient, blockgameClient);

    ClientPlayConnectionEvents.JOIN.register(this::handleJoin);
    ClientPlayConnectionEvents.DISCONNECT.register(this::handleDisconnect);
    ScreenOpenedListener.EVENT.register(this::handleScreen);
    ScreenReceivedInventoryListener.EVENT.register(this::handleScreenInventory);
    SlotClickedListener.EVENT.register(this::handleSlotClicked);
    EntityAttackedListener.EVENT.register(this::handleEntityAttacked);
    ClientCommandRegistrationCallback.EVENT.register(this::registerCommand);
    ScreenEvents.AFTER_INIT.register(this::handleScreenInit);

    ClientTickEvents.START_WORLD_TICK.register((client) -> {
      if (Journal.INSTANCE != null) {
        Journal.INSTANCE.getMetadata().incrementLoadedTime();
      }
    });

    Storage.setup();
  }

  @Override
  public void tick(MinecraftClient client) {
    if (client.currentScreen == null && client.getOverlay() == null) {
      if (OPEN_GUI.wasPressed()) {
        this.openScreen(client, null);
      }
    }
  }

  private void openScreen(MinecraftClient client, @Nullable Screen parent) {
    client.setScreen(new RecipeJournalScreen(parent));
  }

  // region Handlers

  private ActionResult handleScreenInventory(InventoryS2CPacket packet) {
    if (packet.getSyncId() == this.recipePreviewHandler.getSyncId()) {
      return this.recipePreviewHandler.handleScreenInventory(packet);
    }

    if (packet.getSyncId() == this.craftingStationHandler.getSyncId()) {
      return this.craftingStationHandler.handleScreenInventory(packet);
    }

    return ActionResult.PASS;
  }

  /**
   * Sets listeningSyncId to this screen's sync id if its name is "Recipe Preview"
   */
  private ActionResult handleScreen(OpenScreenS2CPacket packet) {
    String screenName = packet.getName().getString();

    // Reset the sync ids
    this.recipePreviewHandler.setSyncId(-1);
    this.craftingStationHandler.setSyncId(-1);

    if (screenName.equals("Recipe Preview")) {
      return this.recipePreviewHandler.handleOpenScreen(packet);
    }

    // Reset recipe page if no longer on a "Recipe Preview" screen
    this.recipePreviewHandler.reset();

    // Look for screen name "Some Name (#page#/#max#)"
    if (screenName.matches("^([\\w\\s]+)\\s\\(\\d+/\\d+\\)")) {
      return this.craftingStationHandler.handleOpenScreen(packet);
    }

    // If the previous check failed, look for screen name "Some Name" where the name contains the last attacked player's name
    if (lastAttackedPlayer != null && screenName.contains(lastAttackedPlayer.getEntityName())) {
      return this.craftingStationHandler.handleOpenScreen(packet);
    }

    return ActionResult.PASS;
  }

  private ActionResult handleSlotClicked(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player) {
    if (syncId == this.recipePreviewHandler.getSyncId()) {
      return this.recipePreviewHandler.handleSlotClicked(syncId, slotId, button, actionType, player);
    }

    if (syncId == this.craftingStationHandler.getSyncId()) {
      return this.craftingStationHandler.handleSlotClicked(syncId, slotId, button, actionType, player);
    }

    return ActionResult.PASS;
  }

  private void handleJoin(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
    // Only load the journal if the player is joining the "mc.blockgame.info" server
    client.execute(() -> {
      ServerInfo serverInfo = client.getCurrentServerEntry();
      if (serverInfo == null) {
        LOGGER.warn("[Blockgame Journal] Not connected to a server");
        return;
      }

      String serverAddress = serverInfo.address;
      if (!serverAddress.equals("mc.blockgame.info")) {
        LOGGER.warn("[Blockgame Journal] Not connected to the Blockgame server");
        return;
      }

      Journal.loadDefault();
    });
  }

  private void handleDisconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
    // Unload the journal when disconnecting from the server
    Journal.unload();
  }

  private void registerCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
    dispatcher.register(ClientCommandManager.literal("journal")
        .executes(context -> {
          if (Journal.INSTANCE == null) {
            context.getSource().sendFeedback(Text.literal("Journal is not loaded").formatted(Formatting.RED));
            return 0;
          }

          context.getSource().getClient().send(() -> {
            this.openScreen(context.getSource().getClient(), null);
          });
          return 1;
        })
        .then(ClientCommandManager.literal("save")
            .executes(context -> {
              if (Journal.INSTANCE == null) {
                context.getSource().sendFeedback(Text.literal("Journal is not loaded").formatted(Formatting.RED));
                return 0;
              }

              Journal.save();
              context.getSource().sendFeedback(Text.literal("Journal saved"));
              return 1;
            })
        )
    );

    dispatcher.register(ClientCommandManager.literal("journal").then(ClientCommandManager.literal("list")
        .executes(context -> {
          if (Journal.INSTANCE == null) {
            context.getSource().sendFeedback(Text.literal("Journal is not loaded").formatted(Formatting.RED));
            return 0;
          }

          var knownItems = Journal.INSTANCE.getKnownItems();
          var entries = Journal.INSTANCE.getEntries();

          entries.forEach((key, value) -> {
            ItemStack item = knownItems.get(key);
            context.getSource().sendFeedback(item.getName());
            value.forEach(entry -> {
              context.getSource().sendFeedback(Text.literal("  From " + entry.getNpcName()));

              entry.getIngredients().forEach((ingredient, count) -> {
                ItemStack ingredientItem = knownItems.get(ingredient);
                MutableText text = Text.literal("    - ");
                text.append(ingredientItem.getName());
                text.append(Text.literal(" x" + count));
                context.getSource().sendFeedback(text);
              });
            });
          });

          return 1;
        }))
    );
  }

  private void handleScreenInit(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight) {
    if (screen instanceof AbstractInventoryScreen<?>) {
      ScreenKeyboardEvents.afterKeyPress(screen).register((parent, key, scancode, modifiers) -> {
        if (screen instanceof CreativeInventoryScreen creativeInventoryScreen) {
          if (creativeInventoryScreen.searchBox.isActive()) {
            return;
          }
        }
        else if (screen instanceof RecipeBookProvider recipeBookProvider) {
          var recipeBook = recipeBookProvider.getRecipeBookWidget();
          if (recipeBook.searchField != null && recipeBook.searchField.isActive()) {
            return;
          }
        }
        else if (screen.getFocused() instanceof TextFieldWidget textFieldWidget) {
          if (textFieldWidget.isActive()) {
            return;
          }
        }

        if (OPEN_GUI.matchesKey(key, scancode)) {
          this.openScreen(client, parent);
        }
      });
    }
  }

  private ActionResult handleEntityAttacked(PlayerEntity playerEntity, Entity entity) {
    if (entity instanceof PlayerEntity) {
      lastAttackedPlayer = (PlayerEntity) entity;
    } else {
      lastAttackedPlayer = null;
    }
    return ActionResult.PASS;
  }

  // endregion Handlers
}
