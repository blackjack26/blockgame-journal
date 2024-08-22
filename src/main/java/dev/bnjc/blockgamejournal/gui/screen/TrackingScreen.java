package dev.bnjc.blockgamejournal.gui.screen;

import dev.bnjc.blockgamejournal.gui.widget.TrackingWidget;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.JournalEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public class TrackingScreen extends Screen {
  private final Screen parent;

  private TrackingWidget trackingWidget;

  public TrackingScreen(Screen parent) {
    super(Text.empty());

    this.parent = parent;
  }

  @Override
  protected void init() {
    if (Journal.INSTANCE == null) {
      // TODO: Show error screen
      MinecraftClient.getInstance().setScreen(null);
      return;
    }

    super.init();

    this.trackingWidget = new TrackingWidget(
        this,
        0,
        0,
        this.width,
        this.height
    );
    this.addDrawableChild(this.trackingWidget);
    this.refreshTracking();
  }

  public void refreshTracking() {
    if (Journal.INSTANCE == null) {
      return;
    }

    var entries = Journal.INSTANCE.getEntries().values().stream()
        .flatMap(List::stream)
        .filter(JournalEntry::isTracked)
        .toList();
    this.trackingWidget.setEntries(entries);

    if (entries.isEmpty()) {
      this.close();
    }
  }

  @Override
  public void close() {
    if (this.client != null) {
      this.client.setScreen(this.parent);
    }
  }
}
