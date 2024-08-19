package dev.bnjc.blockgamejournal.mixin.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
  @Unique
  private static final Identifier DEV_CAPE = new Identifier("blockgamejournal", "textures/cape/devcape.png");

  @Shadow public abstract GameProfile getProfile();

  @Shadow @Final private Supplier<SkinTextures> texturesSupplier;

  @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
  public void getSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
    Identifier capeTexture = null;

    // Set cape texture
    String username = this.getProfile().getName();

    switch (username) {
      case "bnjc" -> capeTexture = DEV_CAPE;
    }

    // Modify outcome if we found a custom cape
    if (capeTexture != null) {
      SkinTextures textures = texturesSupplier.get();
      cir.setReturnValue(new SkinTextures(textures.texture(), textures.textureUrl(), capeTexture, capeTexture, textures.model(), textures.secure()));
    }
  }
}
