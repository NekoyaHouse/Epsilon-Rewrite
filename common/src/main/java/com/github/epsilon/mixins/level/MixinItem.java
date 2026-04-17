package com.github.epsilon.mixins.level;

import com.github.epsilon.managers.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Item.class)
public class MixinItem {

    @Redirect(method = "getPlayerPOVHitResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private static float modifyPlayerPOVHitResultYaw(Player player) {
        if (player == Minecraft.getInstance().player) {
            return RotationManager.INSTANCE.getYaw();
        }
        return player.getYRot();
    }

    @Redirect(method = "getPlayerPOVHitResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getXRot()F"))
    private static float modifyPlayerPOVHitResultPitch(Player player) {
        if (player == Minecraft.getInstance().player) {
            return RotationManager.INSTANCE.getPitch();
        }
        return player.getXRot();
    }

}
