package dev.bnjc.blockgamejournal.gui.toast;

import dev.bnjc.blockgamejournal.journal.metadata.JournalAdvancement;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

@Environment(value = EnvType.CLIENT)
public class EasterEggToast implements Toast {
  private static final Identifier TEXTURE = GuiUtil.sprite("toast/advancement");
  private static final int DEFAULT_DURATION_MS = 5000;

  private final String title;
  private final String description;
  private final ItemStack icon;
  private boolean soundPlayed;

  public EasterEggToast(String title, String description, ItemStack icon) {
    this.title = title;
    this.description = description;
    this.icon = icon;

    this.soundPlayed = false;
  }

  @Override
  public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
    context.drawGuiTexture(TEXTURE, 0, 0, this.getWidth(), this.getHeight());

    List<OrderedText> list = manager.getClient().textRenderer.wrapLines(Text.literal(description), 125);
    if (list.size() == 1) {
      // Advancement Made!
      context.drawText(manager.getClient().textRenderer, Text.literal(title), 30, 7, 0xFFFF00 | 0xFF000000, false);
      context.drawText(manager.getClient().textRenderer, list.get(0), 30, 18, -1, false);
    }
    else {
      long NEXT_TIME = 1500L;
      float f = 300.f;

      if (startTime < NEXT_TIME) {
        int k = MathHelper.floor(MathHelper.clamp((float)(NEXT_TIME - startTime) / f, 0.0f, 1.0f) * 255.0f) << 24 | 0x4000000;
        context.drawText(manager.getClient().textRenderer, Text.of(title), 30, 11, 0xFFFF00 | k, false);
      } else {
        int k = MathHelper.floor(MathHelper.clamp((float)(startTime - NEXT_TIME) / f, 0.0f, 1.0f) * 252.0f) << 24 | 0x4000000;
        int l = this.getHeight() / 2 - list.size() * manager.getClient().textRenderer.fontHeight / 2;
        for (OrderedText orderedText : list) {
          context.drawText(manager.getClient().textRenderer, orderedText, 30, l, 0xFFFFFF | k, false);
          l += manager.getClient().textRenderer.fontHeight;
        }
      }
    }

    if (!this.soundPlayed && startTime > 0L) {
      this.soundPlayed = true;
      manager.getClient().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f));
    }

    context.drawItemWithoutEntity(icon, 8, 8);
    return (double)startTime >= DEFAULT_DURATION_MS * manager.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
  }

  public static void show(JournalAdvancement advancement) {
    show(advancement.getTitle(), advancement.getDescription(), advancement.getIcon());
  }

  public static void show(String title, String description, ItemStack icon) {
    MinecraftClient.getInstance().getToastManager().add(new EasterEggToast(title, description, icon));
  }
}
