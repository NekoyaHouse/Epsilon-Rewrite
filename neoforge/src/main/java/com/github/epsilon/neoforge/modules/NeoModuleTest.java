package com.github.epsilon.neoforge.modules;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;

public class NeoModuleTest extends Module {

    public static NeoModuleTest INSTANCE = new NeoModuleTest();

    private NeoModuleTest() {
        super("Neo Module Test", Category.COMBAT);
    }

}
