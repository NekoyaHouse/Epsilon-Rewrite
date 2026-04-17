package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.input.KeyboardInputEvent;
import com.github.epsilon.events.movement.MotionEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import com.github.epsilon.events.bus.EventHandler;

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

    @EventHandler
    private void onMotion(MotionEvent event) {
        if (nullCheck()) return;

        if (mc.player.fallDistance > fallDistance.getValue()) {
            flag = true;
        }

        if (flag && mc.player.onGround()) {
            switch (mode.getValue()) {
                case GroundSpoof -> event.setOnGround(false);
                case Packet -> mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(false, false));
                case GrimMotion -> {
                    event.setY(event.getY() + 0.1);
                    jump = true;
                }
            }
            flag = false;
        }
    }

    @EventHandler
    private void onMovementInputEvent(KeyboardInputEvent event) {
        if (jump) {
            event.setJump(true);
            jump = false;
        }
    }

}
