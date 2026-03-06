package dev.sakura.server.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public final class UnsafeReflect {
    public static final Unsafe theUnsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            theUnsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UnsafeReflect() {
    }
}

