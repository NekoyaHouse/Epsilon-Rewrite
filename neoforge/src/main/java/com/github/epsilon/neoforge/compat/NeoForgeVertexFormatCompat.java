package com.github.epsilon.neoforge.compat;

import com.github.epsilon.compat.VertexFormatCompat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class NeoForgeVertexFormatCompat implements VertexFormatCompat {

    @Override
    public int findNextVertexFormatElementId() {
        return VertexFormatElement.findNextId();
    }

    @Override
    public VertexFormatElement registerVertexFormatElement(int id, int index, VertexFormatElement.Type type, boolean normalized, int count) {
        return VertexFormatElement.register(id, index, type, normalized, count);
    }

}

