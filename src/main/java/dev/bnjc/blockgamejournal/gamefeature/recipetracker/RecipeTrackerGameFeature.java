package dev.bnjc.blockgamejournal.gamefeature.recipetracker;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.client.BlockgameJournalClient;
import dev.bnjc.blockgamejournal.gamefeature.GameFeature;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers.BackpackHandler;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers.ProfileHandler;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers.vendor.VendorHandler;
import dev.bnjc.blockgamejournal.gui.screen.JournalScreen;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.npc.NPCEntry;
import dev.bnjc.blockgamejournal.listener.chat.ReceiveChatListener;
import dev.bnjc.blockgamejournal.listener.interaction.ItemInteractListener;
import dev.bnjc.blockgamejournal.listener.renderer.PostRenderListener;
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
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecipeTrackerGameFeature extends GameFeature {
  private static final Logger LOGGER = BlockgameJournal.getLogger("Recipe Tracker");

  private static final KeyBinding OPEN_GUI = KeyBindingHelper.registerKeyBinding(
      new KeyBinding("key.blockgamejournal.open_gui", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_J, "category.blockgamejournal")
  );

  private static final Pattern BALANCE_PATTERN = Pattern.compile("Balance: ([\\d,]+(?:\\.\\d+)?)\\$");

  @Getter
  private final VendorHandler vendorHandler;

  @Getter
  private final ProfileHandler profileHandler;

  @Getter
  private final BackpackHandler backpackHandler;

  @Getter
  @Nullable
  private Screen lastScreen = null;

  @Getter
  @Nullable
  private String lastRecipeName = null;

  public RecipeTrackerGameFeature() {
    this.vendorHandler = new VendorHandler();
    this.profileHandler = new ProfileHandler(this);
    this.backpackHandler = new BackpackHandler(this);
  }

  @Override
  public void init(MinecraftClient minecraftClient, BlockgameJournalClient blockgameClient) {
    super.init(minecraftClient, blockgameClient);

    ClientPlayConnectionEvents.JOIN.register(this::handleJoin);
    ClientPlayConnectionEvents.DISCONNECT.register(this::handleDisconnect);
    ItemInteractListener.EVENT.register(this::handleItemInteract);
    ClientCommandRegistrationCallback.EVENT.register(this::registerCommand);
    ScreenEvents.AFTER_INIT.register(this::handleScreenInit);
    ReceiveChatListener.EVENT.register(this::handleChatMessage);
    PostRenderListener.EVENT.register(this::handlePostRender);

    ClientTickEvents.START_WORLD_TICK.register((client) -> {
      if (Journal.INSTANCE != null) {
        Journal.INSTANCE.getMetadata().incrementLoadedTime();
      }
    });

    this.vendorHandler.init();
    this.profileHandler.init();
    this.backpackHandler.init();

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
    client.setScreen(new JournalScreen(parent));
  }

  // region Handlers

  private void handleJoin(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
    // Only load the journal if the player is joining the "mc.blockgame.info" server
    client.execute(() -> {
      // TODO: Uncomment
//      ServerInfo serverInfo = client.getCurrentServerEntry();
//      if (serverInfo == null) {
//        LOGGER.warn("[Blockgame Journal] Not connected to a server");
//        return;
//      }
//
//      String serverAddress = serverInfo.address;
//      if (!serverAddress.equals("mc.blockgame.info")) {
//        LOGGER.warn("[Blockgame Journal] Not connected to the Blockgame server");
//        return;
//      }

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
    this.lastScreen = screen;

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

  private ActionResult handleChatMessage(MinecraftClient client, String message) {
    if (Journal.INSTANCE == null) {
      return ActionResult.PASS;
    }

    String cleanedMessage = message.replaceAll("[§&][0-9a-f]", "");

    // Parse "Balance: 19,611.26$" to 19611.26
    Matcher matcher = BALANCE_PATTERN.matcher(cleanedMessage);
    if (matcher.find()) {
      String balanceString = matcher.group(1);
      Journal.INSTANCE.getMetadata().setPlayerBalance(Float.parseFloat(balanceString.replace(",", "")));
      return ActionResult.PASS;
    }

    if (cleanedMessage.startsWith("[RECIPE]") && lastRecipeName != null) {
      Journal.INSTANCE.getMetadata().setKnownRecipe("mmoitems:" + lastRecipeName, true);
    }

    return ActionResult.PASS;
  }

  private ActionResult handleItemInteract(PlayerEntity playerEntity, Hand hand) {
    ItemStack itemStack = playerEntity.getStackInHand(hand);

    NbtCompound nbt = itemStack.getNbt();
    if (nbt != null && nbt.contains("MMOITEMS_ITEM_TYPE") && nbt.getString("MMOITEMS_ITEM_TYPE").equals("RECIPE")) {
      // Usually formatted like "blockgame.recipe.SANCTIFIED_BOOTS"
      String permission = nbt.getString("MMOITEMS_PERMISSION");
      lastRecipeName = permission.substring(permission.lastIndexOf(".") + 1);
    } else {
      lastRecipeName = null;
    }

    return ActionResult.PASS;
  }

  // TODO: Move into another game feature
  private void handlePostRender(float tickDelta, long limitTime, MatrixStack matrices, boolean withDepth, boolean withoutDepth) {
    if (Journal.INSTANCE == null) {
      return;
    }

    Map<String, NPCEntry> knownNPCs = Journal.INSTANCE.getKnownNPCs();
    if (knownNPCs.isEmpty()) {
      return;
    }

    // TODO: Sort waypoints
    Entity cameraEntity = this.getMinecraftClient().getCameraEntity();
    if (cameraEntity == null) {
      return;
    }

    double renderPosX = cameraEntity.getX();
    double renderPosY = cameraEntity.getY();
    double renderPosZ = cameraEntity.getZ();

    RenderSystem.enableCull();

    // TODO: If enabled
    RenderSystem.enableBlend();
    RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
        GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

    String worldName = "";
    if (this.getMinecraftClient().world != null) {
      worldName = this.getMinecraftClient().world.getRegistryKey().getValue().toString();
    }

    for (NPCEntry entry : knownNPCs.values()) {
      if (entry.getPosition() == null || !entry.isLocating() || !worldName.equals(entry.getWorld())) {
        continue;
      }

      double x = entry.getX();
      double y = entry.getY();
      double z = entry.getZ();

      double distance = Math.sqrt(entry.getDistanceSqToEntity(cameraEntity));

      int maxDistance = 10_000; // TODO: Configurable
      if ((distance < maxDistance || maxDistance < 0 /* || entry == this.highlightedWaypoint */) && !this.getMinecraftClient().options.hudHidden) {
        boolean isPointedAt = this.isPointedAt(entry, distance, cameraEntity, tickDelta);
        String label = entry.getName();
        this.renderLabel(matrices, entry, distance, isPointedAt, label, x - renderPosX, y - renderPosY - 0.5, z - renderPosZ, 64, withDepth, withoutDepth);
      }
    }

    // TODO: Highlighted waypoint?

    RenderSystem.enableDepthTest();
    RenderSystem.depthMask(true);
    RenderSystem.disableBlend();
  }
  // endregion Handlers

  // region Helpers
  private boolean isPointedAt(NPCEntry entry, double distance, Entity cameraEntity, float tickDelta) {
    Vec3d cameraPos = cameraEntity.getCameraPosVec(tickDelta);
    double degrees = 5.0 + Math.min(5.0 / distance, 5.0);
    double angle = degrees * 0.174533;
    double size = Math.sin(angle) * distance;
    Vec3d cameraPosPlusDirection = cameraEntity.getRotationVec(tickDelta);
    Vec3d cameraPosPlusDirectionTimesDistance = cameraPos.add(cameraPosPlusDirection.x * distance, cameraPosPlusDirection.y * distance, cameraPosPlusDirection.z * distance);
    Box axisalignedbb = new Box((entry.getX() + 0.5f) - size, (entry.getY() + 1.5f) - size, (entry.getZ() + 0.5f) - size, (entry.getX() + 0.5f) + size, (entry.getY() + 1.5f) + size, (entry.getZ() + 0.5f) + size);
    Optional<Vec3d> raytaceresult = axisalignedbb.raycast(cameraPos, cameraPosPlusDirectionTimesDistance);
    if (axisalignedbb.contains(cameraPos)) {
      return distance >= 1.0;
    } else {
      return raytaceresult.isPresent();
    }
  }

  private void renderLabel(MatrixStack matrices, NPCEntry entry, double distance, boolean isPointedAt, String name,
                           double baseX, double baseY, double baseZ, int par9, boolean withDepth, boolean withoutDepth) {
    name = name + " (" + (int) distance + "m)";

    double maxDistance = this.getMinecraftClient().options.getSimulationDistance().getValue() * 16.0 * 0.99;
    double adjustedDistance = distance;
    if (distance > maxDistance) {
      baseX = baseX / distance * maxDistance;
      baseY = baseY / distance * maxDistance;
      baseZ = baseZ / distance * maxDistance;
      adjustedDistance = maxDistance;
    }

    float var14 = ((float) adjustedDistance * 0.1f + 1.0f) * 0.0266f;
    matrices.push();
    matrices.translate((float) baseX + 0.5f, (float) baseY + 0.5f, (float) baseZ + 0.5f);
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-this.getMinecraftClient().getEntityRenderDispatcher().camera.getYaw()));
    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.getMinecraftClient().getEntityRenderDispatcher().camera.getPitch()));
    matrices.scale(-var14, -var14, var14);

    Matrix4f matrix4f = matrices.peek().getPositionMatrix();
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder vertexBuffer = tessellator.getBuffer();
    float fade = distance > 5.0 ? 1.0f : (float) distance / 5.0f;
    float width = 10.0f;
    float r = 1.0f;
    float g = 0.0f;
    float b = 0.0f;

    // TODO: Icon?

    TextRenderer textRenderer = this.getMinecraftClient().textRenderer;
    if (isPointedAt && textRenderer != null) {
      byte elevateBy = -19;
      RenderSystem.enablePolygonOffset();
      int halfStringWidth= textRenderer.getWidth(name) / 2;
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);

      if (withDepth) {
        RenderSystem.depthMask(distance < maxDistance);
        RenderSystem.enableDepthTest();
        RenderSystem.polygonOffset(1.0f, 7.0f);
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        vertexBuffer.vertex(matrix4f, (-halfStringWidth - 2), (-2 + elevateBy), 0.0F).color(r, g, b, 0.6F * fade).next();
        vertexBuffer.vertex(matrix4f, (-halfStringWidth - 2), (9 + elevateBy), 0.0F).color(r, g, b, 0.6F * fade).next();
        vertexBuffer.vertex(matrix4f, (halfStringWidth + 2), (9 + elevateBy), 0.0F).color(r, g, b, 0.6F * fade).next();
        vertexBuffer.vertex(matrix4f, (halfStringWidth + 2), (-2 + elevateBy), 0.0F).color(r, g, b, 0.6F * fade).next();
        tessellator.draw();

        RenderSystem.polygonOffset(1.0f, 5.0f);
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        vertexBuffer.vertex(matrix4f, (-halfStringWidth - 1), (-1 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.6F * fade).next();
        vertexBuffer.vertex(matrix4f, (-halfStringWidth - 1), (8 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.6F * fade).next();
        vertexBuffer.vertex(matrix4f, (halfStringWidth + 1), (8 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.6F * fade).next();
        vertexBuffer.vertex(matrix4f, (halfStringWidth + 1), (-1 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.6F * fade).next();
        tessellator.draw();
      }

      if (withoutDepth) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.polygonOffset(1.0f, 11.0f);
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        vertexBuffer.vertex(matrix4f, (-halfStringWidth - 2), (-2 + elevateBy), 0.0F).color(r, g, b, 0.15F * fade).next();
        vertexBuffer.vertex(matrix4f, (-halfStringWidth - 2), (9 + elevateBy), 0.0F).color(r, g, b, 0.15F * fade).next();
        vertexBuffer.vertex(matrix4f, (halfStringWidth + 2), (9 + elevateBy), 0.0F).color(r, g, b, 0.15F * fade).next();
        vertexBuffer.vertex(matrix4f, (halfStringWidth + 2), (-2 + elevateBy), 0.0F).color(r, g, b, 0.15F * fade).next();
        tessellator.draw();

        RenderSystem.polygonOffset(1.0f, 9.0f);
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        vertexBuffer.vertex(matrix4f, (-halfStringWidth - 1), (-1 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.15F * fade).next();
        vertexBuffer.vertex(matrix4f, (-halfStringWidth - 1), (8 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.15F * fade).next();
        vertexBuffer.vertex(matrix4f, (halfStringWidth + 1), (8 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.15F * fade).next();
        vertexBuffer.vertex(matrix4f, (halfStringWidth + 1), (-1 + elevateBy), 0.0F).color(0.0F, 0.0F, 0.0F, 0.15F * fade).next();
        tessellator.draw();
      }

      RenderSystem.disablePolygonOffset();
      RenderSystem.depthMask(false);

      VertexConsumerProvider.Immediate vertexConsumerProvider = this.getMinecraftClient().getBufferBuilders().getEffectVertexConsumers();
      if (withoutDepth) {
        int textColor = (int) (255.0f * fade) << 24 | 0xFFFFFF;
        RenderSystem.disableDepthTest();
        textRenderer.draw(Text.literal(name), (-textRenderer.getWidth(name) / 2f), elevateBy, textColor, false, matrix4f, vertexConsumerProvider, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
        vertexConsumerProvider.draw();
      }

      RenderSystem.enableBlend();
    }

    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    matrices.pop();
  }
  // endregion Helpers
}
