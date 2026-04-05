package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.MotionEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.neoforged.bus.api.SubscribeEvent;

public class NoFall extends Module {

    public static final NoFall INSTANCE = new NoFall();

    private NoFall() {
        super("NoFall", Category.PLAYER);
    }

    private enum Mode {
        GroundSpoof,
        Packet,
        GrimMotion
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.GroundSpoof);
    private final DoubleSetting fallDistance = doubleSetting("Fall Distance", 3, 3, 16, 1);

    private boolean flag;
    private boolean jump;

    @SubscribeEvent
    private void onMotion(MotionEvent e) {
        if (nullCheck()) return;

        if (mc.player.fallDistance > fallDistance.getValue()) {
            flag = true;
        }

        if (flag && mc.player.onGround()) {
            switch (mode.getValue()) {
                case GroundSpoof -> e.setOnGround(false);
                case Packet -> {
                    mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(false, false));
                }
                case GrimMotion -> {
                    // TODO :你的moveinputEvent并不支持跳跃所以没触发
                    e.setY(e.getY() + 0.1f);
                    jump = true;
                }
            }
            flag = false;
        }
    }

}
