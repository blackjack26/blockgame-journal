package dev.bnjc.blockgamejournal.gui.toast;

import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.journal.npc.NPCEntry;
import dev.bnjc.blockgamejournal.util.GuiUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

@Environment(value = EnvType.CLIENT)
public class VendorToast implements Toast {
  private static final Identifier TEXTURE = GuiUtil.sprite("toast/vendor");
  private static final long DEFAULT_DURATION_MS = 5000L;

  private final NPCEntry vendorEntry;

  private long startTime;
  private boolean justUpdated;

  public VendorToast(NPCEntry vendorEntry) {
    this.vendorEntry = vendorEntry;
  }

  @Override

  public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
    if (this.justUpdated) {
      this.startTime = startTime;
      this.justUpdated = false;
    }

    context.drawGuiTexture(TEXTURE, 0, 0, this.getWidth(), this.getHeight());
    context.drawText(manager.getClient().textRenderer, "New Vendor Found!", 30, 7, -11534256, false);
    context.drawText(manager.getClient().textRenderer, vendorEntry.getName(), 30, 18, -16777216, false);

    if (Journal.INSTANCE != null) {
      ItemStack stack = Journal.INSTANCE.getKnownNpcItem(vendorEntry.getName());
      if (stack == null) {
        stack = new ItemStack(Items.PLAYER_HEAD);
      }

      context.drawItemWithoutEntity(stack, 8, 8);
    }

    return (double)(startTime - this.startTime) >= DEFAULT_DURATION_MS * manager.getNotificationDisplayTimeMultiplier()
        ? Toast.Visibility.HIDE
        : Toast.Visibility.SHOW;
  }

  public static void show(ToastManager manager, NPCEntry vendorEntry) {
    manager.add(new VendorToast(vendorEntry));
  }
}
