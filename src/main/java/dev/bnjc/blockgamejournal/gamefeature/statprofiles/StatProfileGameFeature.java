package dev.bnjc.blockgamejournal.gamefeature.statprofiles;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.client.BlockgameJournalClient;
import dev.bnjc.blockgamejournal.gamefeature.GameFeature;
import dev.bnjc.blockgamejournal.gui.screen.StatScreen;
import dev.bnjc.blockgamejournal.gui.widget.stats.TalentListWidget;
import dev.bnjc.blockgamejournal.listener.screen.ScreenOpenedListener;
import dev.bnjc.blockgamejournal.listener.screen.ScreenReceivedInventoryListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class StatProfileGameFeature extends GameFeature {
  private static final Logger LOGGER = BlockgameJournal.getLogger("Stat Profile");

  @Nullable
  private StatScreen screen;

  @Override
  public void init(MinecraftClient minecraftClient, BlockgameJournalClient blockgameClient) {
    super.init(minecraftClient, blockgameClient);

    ScreenOpenedListener.EVENT.register(this::handleScreenOpened);
    ScreenReceivedInventoryListener.EVENT.register(this::handleScreenReceivedInventory);
  }

  public void handleScreenClose() {
    this.screen = null;
    TalentListWidget.lastScrollY = 0;
  }

  private ActionResult handleScreenOpened(OpenScreenS2CPacket packet) {
    String screenName = packet.getName().getString();

    if (screenName.equals("Talents") && BlockgameJournal.getConfig().getStatConfig().enableEnhancedStats) {
      if (this.screen == null) {
        this.screen = new StatScreen(packet.getName(), packet.getSyncId(), this);
      } else {
        this.screen = this.screen.clone(packet);
      }

      ClientPlayerEntity player = this.getMinecraftClient().player;
      if (player != null) {
        player.currentScreenHandler = ScreenHandlerType.GENERIC_9X6.create(packet.getSyncId(), player.getInventory());
        this.getMinecraftClient().setScreen(this.screen);
      }
      return ActionResult.SUCCESS;
    }

    return ActionResult.PASS;
  }

  private ActionResult handleScreenReceivedInventory(InventoryS2CPacket packet) {
    if (this.screen != null && packet.getSyncId() == this.screen.getSyncId()) {
      this.screen.setInventory(packet.getContents());
      return ActionResult.SUCCESS;
    }

    return ActionResult.PASS;
  }
}
