package dev.bnjc.blockgamejournal.gamefeature.recipetracker;

import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TrackRecipeItem {
  @Getter
  private final ItemStack itemStack;

  public TrackRecipeItem() {
    this.itemStack = new ItemStack(Items.WRITABLE_BOOK);
    this.updateFormatting();
  }

  private void updateFormatting() {
    // Add name to the tracking item
    MutableText nameText = Text.literal("Track Recipe");
    nameText.setStyle(nameText.getStyle().withItalic(false).withFormatting(Formatting.GREEN));
    this.itemStack.setCustomName(nameText);

    // Add lore to the tracking item
    MutableText loreText = Text.literal("Start tracking materials for this recipe.");
    loreText.setStyle(loreText.getStyle().withItalic(false).withFormatting(Formatting.GRAY));

    NbtList lore = new NbtList();
    lore.add(NbtString.of(Text.Serializer.toJson(loreText)));
    this.itemStack.getOrCreateSubNbt("display").put("Lore", lore);
  }
}
