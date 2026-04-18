package com.github.epsilon.graphics.vulkan.descriptor;

import static org.lwjgl.vulkan.VK12.*;

public record DescriptorBindingSpec(int binding, int descriptorType, int descriptorCount, int stageFlags) {

    public DescriptorBindingSpec {
        if (binding < 0) {
            throw new IllegalArgumentException("binding must be >= 0");
        }
        if (descriptorCount <= 0) {
            throw new IllegalArgumentException("descriptorCount must be > 0");
        }
    }

    public static DescriptorBindingSpec ssbo(int binding) {
        return new DescriptorBindingSpec(binding, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, 1, VK_SHADER_STAGE_COMPUTE_BIT);
    }

    public static DescriptorBindingSpec ssbo(int binding, int descriptorCount, int stageFlags) {
        return new DescriptorBindingSpec(binding, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, descriptorCount, stageFlags);
    }

    public static DescriptorBindingSpec uniformBuffer(int binding, int descriptorCount, int stageFlags) {
        return new DescriptorBindingSpec(binding, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, descriptorCount, stageFlags);
    }

    public static DescriptorBindingSpec combinedImageSampler(int binding, int descriptorCount, int stageFlags) {
        return new DescriptorBindingSpec(binding, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, descriptorCount, stageFlags);
    }

    public static DescriptorBindingSpec sampledImage(int binding, int descriptorCount, int stageFlags) {
        return new DescriptorBindingSpec(binding, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, descriptorCount, stageFlags);
    }

    public static DescriptorBindingSpec storageImage(int binding, int descriptorCount, int stageFlags) {
        return new DescriptorBindingSpec(binding, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, descriptorCount, stageFlags);
    }
}

