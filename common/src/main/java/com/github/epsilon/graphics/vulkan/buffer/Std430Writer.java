package com.github.epsilon.graphics.vulkan.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class Std430Writer {

    private final ByteBuffer target;

    public Std430Writer(ByteBuffer target) {
        this.target = target.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void clear() {
        target.clear();
    }

    public int position() {
        return target.position();
    }

    public int writtenBytes() {
        return target.position();
    }

    public Std430Writer align(int alignment) {
        if (alignment <= 0 || (alignment & (alignment - 1)) != 0) {
            throw new IllegalArgumentException("alignment must be a positive power of two");
        }

        int p = target.position();
        int aligned = (p + alignment - 1) & -alignment;
        while (target.position() < aligned) {
            target.put((byte) 0);
        }
        return this;
    }

    public Std430Writer putInt(int value) {
        align(4);
        target.putInt(value);
        return this;
    }

    public Std430Writer putUInt(int value) {
        return putInt(value);
    }

    public Std430Writer putFloat(float value) {
        align(4);
        target.putFloat(value);
        return this;
    }

    public Std430Writer putVec2(float x, float y) {
        align(8);
        target.putFloat(x).putFloat(y);
        return this;
    }

    public Std430Writer putVec3(float x, float y, float z) {
        // std430 vec3 has 16-byte base alignment in structs.
        align(16);
        target.putFloat(x).putFloat(y).putFloat(z).putFloat(0.0f);
        return this;
    }

    public Std430Writer putVec4(float x, float y, float z, float w) {
        align(16);
        target.putFloat(x).putFloat(y).putFloat(z).putFloat(w);
        return this;
    }

    public Std430Writer putMat4(float[] matrix16) {
        if (matrix16.length != 16) {
            throw new IllegalArgumentException("mat4 expects 16 floats");
        }
        align(16);
        for (float value : matrix16) {
            target.putFloat(value);
        }
        return this;
    }

    public Std430Writer putBytes(ByteBuffer src, int alignment) {
        align(alignment);
        target.put(src.duplicate());
        return this;
    }
}

