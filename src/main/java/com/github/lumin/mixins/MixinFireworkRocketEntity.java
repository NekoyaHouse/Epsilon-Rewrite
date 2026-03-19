package com.github.lumin.mixins;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity {

    @Shadow
    private int life;

    @Shadow
    private int lifetime;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {

    }

}
