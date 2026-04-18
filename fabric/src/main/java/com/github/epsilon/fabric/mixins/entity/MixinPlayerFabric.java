package com.github.epsilon.fabric.mixins.entity;

import com.github.epsilon.managers.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public class MixinPlayerFabric {

    @Redirect(method = "doSweepAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float modifySweepAttackYaw(Player player) {
        if (player == Minecraft.getInstance().player) {
            return RotationManager.INSTANCE.getYaw();
        }
        return player.getYRot();
    }

}

