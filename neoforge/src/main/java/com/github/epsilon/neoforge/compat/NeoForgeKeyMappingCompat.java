package com.github.epsilon.neoforge.compat;

import com.github.epsilon.compat.KeyMappingCompat;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public class NeoForgeKeyMappingCompat implements KeyMappingCompat {

    @Override
    public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
        return keyMapping.getKey();
    }

}

