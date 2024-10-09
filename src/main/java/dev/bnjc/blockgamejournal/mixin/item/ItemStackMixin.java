package dev.bnjc.blockgamejournal.mixin.item;

import dev.bnjc.blockgamejournal.BlockgameJournal;
import dev.bnjc.blockgamejournal.gamefeature.recipetracker.handlers.vendor.VendorHandler;
import dev.bnjc.blockgamejournal.journal.Journal;
import dev.bnjc.blockgamejournal.util.ItemUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
  @Shadow public abstract @Nullable NbtCompound getNbt();

  @Inject(method = "getTooltip", at = @At("TAIL"))
  private void getTooltip(@Nullable PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> ci) {
    try {
      if (Journal.INSTANCE == null) {
        return;
      }

      NbtCompound nbt = this.getNbt();
      if (nbt == null) {
        return;
      }

      if (nbt.contains("MMOITEMS_ITEM_TYPE") && nbt.getString("MMOITEMS_ITEM_TYPE").equals("RECIPE")) {
        String permission = nbt.getString("MMOITEMS_PERMISSION");
        String lastRecipeName = permission.substring(permission.lastIndexOf(".") + 1);

        Boolean knownRecipe = Journal.INSTANCE.getMetadata().getKnownRecipe("mmoitems:" + lastRecipeName);
        if (knownRecipe != null) {
          ci.getReturnValue().add(2, Text.empty());

          MutableText indicatorText = Text.literal(knownRecipe ? "✔ " : "✖ ").formatted(knownRecipe ? Formatting.GREEN : Formatting.RED);
          if (knownRecipe) {
            indicatorText.append(Text.literal("You know this recipe!").formatted(Formatting.GREEN));
          } else {
            indicatorText.append(Text.literal("You don't know this recipe!").formatted(Formatting.RED));
          }
          ci.getReturnValue().add(3, indicatorText);
        }
      }

      var key = ItemUtil.getKey((ItemStack) (Object) this);
      if (key != null) {
        var outdatedInfo = VendorHandler.getOutdatedItemInfo().get(key);
        if (outdatedInfo != null) {
          ci.getReturnValue().add(Text.empty());
          ci.getReturnValue().add(Text.literal("Your journal entry is outdated!").formatted(Formatting.GOLD));

          outdatedInfo.forEach((info) -> {
            ci.getReturnValue().add(info);
          });
        }
      }

    } catch (Exception e) {
      BlockgameJournal.LOGGER.error("[Blockgame Journal] Failed to add tooltip", e);
    }
  }
}
