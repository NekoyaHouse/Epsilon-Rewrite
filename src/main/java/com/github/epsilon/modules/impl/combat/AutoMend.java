package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.utils.player.FindItemResult;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.rotation.Priority;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector2f;

public class AutoMend extends Module {

    public static final AutoMend INSTANCE = new AutoMend();

    private AutoMend() {
        super("AutoMend", Category.COMBAT);
    }

    @SubscribeEvent
    private void onClientTick(ClientTickEvent.Pre event) {
        FindItemResult result = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
        if (!result.found()) return;

        RotationManager.INSTANCE.applyRotation(new Vector2f(mc.player.getYRot(), 90), 10, Priority.High, rotationApplyRecord -> {
            if (nullCheck() || !isEnabled()) return;
            InvUtils.swap(result.slot(), true);
            mc.gameMode.useItem(mc.player, result.getHand());
            InvUtils.swapBack();
        });
    }

}
