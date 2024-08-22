package dev.bnjc.blockgamejournal.journal.metadata;

import com.mojang.serialization.Codec;
import dev.bnjc.blockgamejournal.gui.toast.EasterEggToast;
import dev.bnjc.blockgamejournal.journal.Journal;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@Getter
public enum JournalAdvancement {
  CARPAL_TUNNEL("Carpal tunnel alert!", "Can you click faster?", new ItemStack(Items.BONE));

  public static final Codec<JournalAdvancement> CODEC = Codec.STRING.xmap(JournalAdvancement::valueOf, JournalAdvancement::name);

  private final String title;
  private final String description;
  private final ItemStack icon;

  JournalAdvancement(String title, String description, ItemStack icon) {
    this.title = title;
    this.description = description;
    this.icon = icon;
  }

  public void grant() {
    if (Journal.INSTANCE == null) {
      return;
    }

    if (Journal.INSTANCE.getMetadata().hasAdvancement(this)) {
      return;
    }

    EasterEggToast.show(this);
    Journal.INSTANCE.getMetadata().grantAdvancement(this);
  }
}
