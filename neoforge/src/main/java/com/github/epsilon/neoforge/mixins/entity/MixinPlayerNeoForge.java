package com.github.epsilon.neoforge.mixins.entity;

import com.github.epsilon.managers.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public class MixinPlayerNeoForge {

    @Redirect(method = "doSweepAttack(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;FLnet/minecraft/world/phys/AABB;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"), require = 0)
    private float modifySweepAttackYawFromPlayer(Player player) {
        if (player == Minecraft.getInstance().player) {
            return RotationManager.INSTANCE.getYaw();
        }
        return player.getYRot();
    }

}

