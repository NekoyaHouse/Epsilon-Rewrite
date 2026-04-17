package com.github.epsilon.mixins.client;

import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.input.KeyInputEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        EpsilonEventBus.INSTANCE.post(new KeyInputEvent(event.key(), event.scancode(), action, event.modifiers()));
    }
}

