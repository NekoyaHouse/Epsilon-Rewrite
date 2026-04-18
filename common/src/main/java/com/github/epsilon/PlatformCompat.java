package com.github.epsilon;

import com.github.epsilon.compat.EquipmentSlotCompat;
import com.github.epsilon.compat.KeyMappingCompat;
import com.github.epsilon.compat.VertexFormatCompat;

/**
 * Platform abstraction facade composed from smaller concern-specific contracts.
 *
 * <p>The active implementation is stored in {@link Epsilon#platform} and must be
 * set by the loader-specific entry point before {@link Epsilon#init()} is called.</p>
 */
public interface PlatformCompat extends KeyMappingCompat, EquipmentSlotCompat, VertexFormatCompat {

}
