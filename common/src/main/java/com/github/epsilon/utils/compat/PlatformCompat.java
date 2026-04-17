package com.github.epsilon.utils.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Compatibility utilities for methods that NeoForge adds to vanilla classes.
 * In a multiloader setup, the common module compiles against NeoForm (unpatched vanilla),
 * so we use reflection or vanilla alternatives for NeoForge-patched methods.
 */
public class PlatformCompat {

    // ========================
    // KeyMapping.getKey()
    // ========================

    private static Field keyMappingKeyField;

    /**
     * Gets the InputConstants.Key from a KeyMapping.
     * NeoForge adds getKey(); vanilla has the field 'key' (private).
     */
    public static InputConstants.Key getKeyMappingKey(net.minecraft.client.KeyMapping keyMapping) {
        try {
            if (keyMappingKeyField == null) {
                // Try NeoForge method first
                try {
                    Method m = net.minecraft.client.KeyMapping.class.getMethod("getKey");
                    return (InputConstants.Key) m.invoke(keyMapping);
                } catch (NoSuchMethodException ignored) {}

                // Fallback: access the 'key' field via reflection
                keyMappingKeyField = net.minecraft.client.KeyMapping.class.getDeclaredField("key");
                keyMappingKeyField.setAccessible(true);
            }
            return (InputConstants.Key) keyMappingKeyField.get(keyMapping);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get key from KeyMapping", e);
        }
    }

    // ========================
    // ItemStack.getEquipmentSlot()
    // ========================

    /**
     * Gets the equipment slot for an ItemStack.
     * NeoForge adds getEquipmentSlot(); vanilla uses Equipable interface.
     */
    public static EquipmentSlot getEquipmentSlot(ItemStack stack) {
        try {
            Method m = ItemStack.class.getMethod("getEquipmentSlot");
            return (EquipmentSlot) m.invoke(stack);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get equipment slot from ItemStack", e);
        }
    }

    // ========================
    // VertexFormatElement.findNextId() & register()
    // ========================

    private static Method findNextIdMethod;
    private static Field byIdField;
    private static Method registerMethod;

    /**
     * Calls VertexFormatElement.findNextId() - a NeoForge-added method.
     * Fallback: manually scan the BY_ID array for the first null slot.
     */
    public static int findNextVertexFormatElementId() {
        try {
            // Try NeoForge method first
            if (findNextIdMethod == null) {
                try {
                    findNextIdMethod = VertexFormatElement.class.getDeclaredMethod("findNextId");
                    findNextIdMethod.setAccessible(true);
                } catch (NoSuchMethodException ignored) {
                    findNextIdMethod = null;
                }
            }
            if (findNextIdMethod != null) {
                return (int) findNextIdMethod.invoke(null);
            }

            // Fallback: replicate NeoForge logic using BY_ID field
            if (byIdField == null) {
                byIdField = VertexFormatElement.class.getDeclaredField("BY_ID");
                byIdField.setAccessible(true);
            }
            VertexFormatElement[] byId = (VertexFormatElement[]) byIdField.get(null);
            for (int i = 0; i < byId.length; i++) {
                if (byId[i] == null) {
                    return i;
                }
            }
            throw new IllegalStateException("VertexFormatElement count limit exceeded");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call VertexFormatElement.findNextId()", e);
        }
    }

    /**
     * Calls VertexFormatElement.register() - a NeoForge-added/private method.
     */
    public static VertexFormatElement registerVertexFormatElement(int id, int index, VertexFormatElement.Type type, boolean normalized, int count) {
        try {
            if (registerMethod == null) {
                registerMethod = VertexFormatElement.class.getDeclaredMethod("register", int.class, int.class, VertexFormatElement.Type.class, boolean.class, int.class);
                registerMethod.setAccessible(true);
            }
            return (VertexFormatElement) registerMethod.invoke(null, id, index, type, normalized, count);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call VertexFormatElement.register()", e);
        }
    }

}



