package com.github.epsilon.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * Loader abstraction for key mapping access differences.
 */
public interface KeyMappingCompat {

    InputConstants.Key getKeyMappingKey(KeyMapping keyMapping);

}

