package dev.bnjc.blockgamejournal.client;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.config.ConfigManager;
import dev.bnjc.blockgamejournal.gamefeature.GameFeature;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.RecipeTrackerGameFeature;
import dev.bnjc.blockgamejournal.gamefeature.statprofiles.StatProfileGameFeature;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockgameJournalClient implements ClientModInitializer {
  @Getter
  private static final ArrayList<GameFeature> loadedGameFeatures = new ArrayList<>();

  @Getter
  private static ConfigManager configManager;

  @Getter
  private List<String> userDisabledGameFeatureNames;

  @Getter
  @Setter
  private static int errors;

  @Getter
  private static final int maxErrorsBeforeCrash = 5;

  @Override
  public void onInitializeClient() {
    BlockgameJournal.LOGGER.info("[Blockgame Journal] Welcome to Blockgame Journal!");

    configManager = new ConfigManager();

    // Load all game features
    parseUserDisabledGameFeatures();
    loadGameFeatures();
  }

  private void parseUserDisabledGameFeatures() {
    String x = System.getProperty("nullgf");
    if (x == null) {
      userDisabledGameFeatureNames = new ArrayList<>();
      return;
    }

    userDisabledGameFeatureNames = Arrays.stream(x.toLowerCase().split(",")).toList();
  }

  private void loadGameFeatures() {
    BlockgameJournal.LOGGER.info("[Blockgame Journal] Loading game features...");

    loadGameFeature(new RecipeTrackerGameFeature());
    loadGameFeature(new StatProfileGameFeature());

    // Tick all game features after client ticks
    ClientTickEvents.END_CLIENT_TICK.register((client) -> {
      client.getProfiler().push("tickGameFeatures");

      for (GameFeature gameFeature : loadedGameFeatures) {
        client.getProfiler().push(gameFeature.getClass().getSimpleName());

        // Try to tick and don't crash if it fails
        try {
          gameFeature.tick(client);
        } catch (Exception e) {
          // Crash if there's been too many errors
          if(errors > maxErrorsBeforeCrash) {
            throw e;
          }

          ClientPlayerEntity player = client.player;
          if(player != null) {
            player.sendMessage(Text.of("§4§l=== PLEASE REPORT THIS AS A BUG ==="), false);
            player.sendMessage(Text.of(String.format("§cAn error occurred in %s!", gameFeature.getClass().getSimpleName())), false);
            player.sendMessage(Text.of(e.getClass().getName() + ": §7" + e.getMessage()), false);
            player.sendMessage(Text.of("§4§l================================="), false);
            errors++;
          }
        }

        client.getProfiler().pop();
      }

      client.getProfiler().pop();
    });
  }

  private void loadGameFeature(GameFeature gameFeature) {
    String name = gameFeature.getClass().getSimpleName().replace("GameFeature", "").toLowerCase();
    String featureName = gameFeature.getClass().getSimpleName().replace("GameFeature", " game feature");
    boolean userDisabled = getUserDisabledGameFeatureNames().contains(name);

    if (!gameFeature.isEnabled() || userDisabled) {
      BlockgameJournal.LOGGER.info("[Blockgame Journal] Skipping load of {} because it's disabled", featureName);
      return;
    }

    BlockgameJournal.LOGGER.info("[Blockgame Journal] Loading {}...", featureName);
    try {
      gameFeature.init(MinecraftClient.getInstance(), this);
      loadedGameFeatures.add(gameFeature);
    } catch (Exception e) {
      BlockgameJournal.LOGGER.error("[Blockgame Journal] Failed to load {}!", featureName, e);
    }
  }
}
