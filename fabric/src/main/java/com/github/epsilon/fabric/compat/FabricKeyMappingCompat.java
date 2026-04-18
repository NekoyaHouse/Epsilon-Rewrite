package com.github.epsilon.fabric.compat;

import com.github.epsilon.compat.KeyMappingCompat;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.lang.reflect.Field;

public class FabricKeyMappingCompat implements KeyMappingCompat {

    private Field keyMappingKeyField;

    @Override
    public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
        try {
            if (keyMappingKeyField == null) {
                keyMappingKeyField = KeyMapping.class.getDeclaredField("key");
                keyMappingKeyField.setAccessible(true);
            }
            return (InputConstants.Key) keyMappingKeyField.get(keyMapping);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get key from KeyMapping", e);
        }
    }

}

