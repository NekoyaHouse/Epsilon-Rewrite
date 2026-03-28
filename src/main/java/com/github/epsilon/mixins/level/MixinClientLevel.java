package com.github.epsilon.mixins.level;


import com.github.epsilon.Epsilon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientLevel.class)
public class MixinClientLevel {

    @Redirect(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void hookTickNonPassenger(Entity instance) {
        if (Epsilon.skipTicks > 0 && instance == Minecraft.getInstance().player) {
            Epsilon.skipTicks--;
        } else {
            instance.tick();
        }
    }

}
