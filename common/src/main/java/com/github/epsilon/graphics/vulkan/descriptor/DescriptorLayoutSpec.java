package com.github.epsilon.graphics.vulkan.descriptor;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK12.VK_SHADER_STAGE_COMPUTE_BIT;

public final class DescriptorLayoutSpec {

    private final List<DescriptorBindingSpec> bindings;

    private DescriptorLayoutSpec(List<DescriptorBindingSpec> bindings) {
        this.bindings = List.copyOf(bindings);
        if (this.bindings.isEmpty()) {
            throw new IllegalArgumentException("At least one descriptor binding is required");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<DescriptorBindingSpec> bindings() {
        return bindings;
    }

    public static final class Builder {

        private final List<DescriptorBindingSpec> bindings = new ArrayList<>();

        public Builder addBinding(DescriptorBindingSpec binding) {
            bindings.add(binding);
            return this;
        }

        public Builder addSsbo(int binding) {
            return addSsbo(binding, 1, VK_SHADER_STAGE_COMPUTE_BIT);
        }

        public Builder addSsbo(int binding, int descriptorCount, int stageFlags) {
            return addBinding(DescriptorBindingSpec.ssbo(binding, descriptorCount, stageFlags));
        }

        public Builder addTexture(int binding, int descriptorCount, int stageFlags) {
            return addBinding(DescriptorBindingSpec.combinedImageSampler(binding, descriptorCount, stageFlags));
        }

        public Builder addSampledImage(int binding, int descriptorCount, int stageFlags) {
            return addBinding(DescriptorBindingSpec.sampledImage(binding, descriptorCount, stageFlags));
        }

        public Builder addStorageImage(int binding, int descriptorCount, int stageFlags) {
            return addBinding(DescriptorBindingSpec.storageImage(binding, descriptorCount, stageFlags));
        }

        public Builder addUniformBuffer(int binding, int descriptorCount, int stageFlags) {
            return addBinding(DescriptorBindingSpec.uniformBuffer(binding, descriptorCount, stageFlags));
        }

        public DescriptorLayoutSpec build() {
            return new DescriptorLayoutSpec(bindings);
        }
    }
}

