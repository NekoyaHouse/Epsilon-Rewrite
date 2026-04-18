package com.github.epsilon.compat;

import com.mojang.blaze3d.vertex.VertexFormatElement;

/**
 * Loader abstraction for custom vertex format element registration.
 */
public interface VertexFormatCompat {

    int findNextVertexFormatElementId();

    VertexFormatElement registerVertexFormatElement(int id, int index, VertexFormatElement.Type type, boolean normalized, int count);

}

