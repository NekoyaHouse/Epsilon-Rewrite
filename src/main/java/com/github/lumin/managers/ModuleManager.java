package com.github.lumin.managers;

import com.github.lumin.modules.Module;
import com.github.lumin.modules.impl.client.ClickGui;
import com.github.lumin.modules.impl.combat.AimAssist;
import com.github.lumin.modules.impl.combat.AntiBot;
import com.github.lumin.modules.impl.combat.AutoClicker;
import com.github.lumin.modules.impl.combat.AutoCrystal;
import com.github.lumin.modules.impl.combat.FakePlayer;
import com.github.lumin.modules.impl.combat.KillAura;
import com.github.lumin.modules.impl.player.*;
import com.github.lumin.modules.impl.render.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    public static final ModuleManager INSTANCE = new ModuleManager();

    private List<Module> modules;

    private ModuleManager() {
    }

    public void initModules() {
        modules = new ArrayList<>();

        modules.add(ClickGui.INSTANCE);

        modules.add(AimAssist.INSTANCE);
        modules.add(AntiBot.INSTANCE);
        modules.add(AutoClicker.INSTANCE);
        modules.add(AutoCrystal.INSTANCE);
        modules.add(FakePlayer.INSTANCE);
        modules.add(KillAura.INSTANCE);

        modules.add(AutoAccount.INSTANCE);
        modules.add(BreakCooldown.INSTANCE);
        modules.add(Disabler.INSTANCE);
        modules.add(InvManager.INSTANCE);
        modules.add(JumpCooldown.INSTANCE);
        modules.add(MoveFix.INSTANCE);
        modules.add(NoSlow.INSTANCE);
        modules.add(SafeWalk.INSTANCE);
        modules.add(Scaffold.INSTANCE);
        modules.add(Speedmine.INSTANCE);
        modules.add(Sprint.INSTANCE);
        modules.add(Stealer.INSTANCE);
        modules.add(Stuck.INSTANCE);
        modules.add(Velocity.INSTANCE);

        modules.add(ESP.INSTANCE);
        modules.add(Fullbright.INSTANCE);
        modules.add(HUD.INSTANCE);
        modules.add(ModuleList.INSTANCE);
        modules.add(Nametags.INSTANCE);
        modules.add(NoRender.INSTANCE);
    }

    public List<Module> getModules() {
        return modules;
    }

    public void onKeyEvent(int keyCode, int action) {
        for (final var module : modules) {
            if (module.getKeyBind() == keyCode) {
                if (module.getBindMode() == Module.BindMode.Hold) {
                    if (action == InputConstants.PRESS || action == InputConstants.REPEAT) {
                        module.setEnabled(true);
                    } else if (action == InputConstants.RELEASE) {
                        module.setEnabled(false);
                    }
                } else {
                    if (action == InputConstants.PRESS) {
                        module.toggle();
                    }
                }
            }
        }
    }

}
