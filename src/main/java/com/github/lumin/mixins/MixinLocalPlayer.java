package com.github.lumin.mixins;

import com.github.lumin.events.MotionEvent;
import com.github.lumin.events.SlowdownEvent;
import com.github.lumin.ducks.ClientPlayerAccess;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer implements ClientPlayerAccess {

    @Unique
    private MotionEvent lumin$motionEvent;

    @Shadow
    private void sendPosition() {
    }

    @Shadow
    public void tick() {
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void onPreSendPosition(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        lumin$motionEvent = NeoForge.EVENT_BUS.post(new MotionEvent(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.onGround()));
        if (lumin$motionEvent.isCanceled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double redirectGetX(LocalPlayer instance) {
        return lumin$motionEvent.getX();
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double redirectGetY(LocalPlayer instance) {
        return lumin$motionEvent.getY();
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double redirectGetZ(LocalPlayer instance) {
        return lumin$motionEvent.getZ();
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float redirectGetYRot(LocalPlayer instance) {
        return lumin$motionEvent.getYaw();
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float redirectGetXRot(LocalPlayer instance) {
        return lumin$motionEvent.getPitch();
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean redirectOnGround(LocalPlayer instance) {
        return lumin$motionEvent.isOnGround();
    }

    @Redirect(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    public boolean onSlowdown(LocalPlayer localPlayer) {
        SlowdownEvent event = NeoForge.EVENT_BUS.post(new SlowdownEvent(localPlayer.isUsingItem()));
        return event.isSlowdown();
    }

    @Override
    public void sendMovementPacketsWrapper() {
        sendPosition();
    }

    @Override
    public void superTick() {
        tick();
    }

    @Override
    public void resetEvent() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        lumin$motionEvent = new MotionEvent(player.getX(), player.getBoundingBox().minY, player.getZ(), player.getYRot(), player.getXRot(), player.onGround());
    }

    @Override
    public void resetRotations() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (lumin$motionEvent == null) return;
        player.setYRot(lumin$motionEvent.getYaw());
        player.setXRot(lumin$motionEvent.getPitch());
    }

}
