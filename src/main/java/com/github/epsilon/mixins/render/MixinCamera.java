package com.github.epsilon.mixins.render;

import com.github.epsilon.modules.impl.render.CameraClip;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class MixinCamera {

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void hookGetMaxZoom(float cameraDist, CallbackInfoReturnable<Float> cir) {
        CameraClip cameraClip = CameraClip.INSTANCE;
        if (cameraClip.isEnabled()) {
            cir.setReturnValue(cameraClip.distance.getValue().floatValue());
        }
    }

}
