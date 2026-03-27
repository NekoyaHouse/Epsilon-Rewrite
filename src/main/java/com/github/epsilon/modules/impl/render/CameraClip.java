package com.github.epsilon.modules.impl.render;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;

public class CameraClip extends Module {

    public static final CameraClip INSTANCE = new CameraClip();

    private CameraClip() {
        super("CameraClip", Category.RENDER);
    }

}
