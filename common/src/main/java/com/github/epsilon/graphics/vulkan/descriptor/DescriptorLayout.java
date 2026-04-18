package com.github.epsilon.graphics.vulkan.descriptor;

import com.mojang.blaze3d.vulkan.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public final class DescriptorLayout implements AutoCloseable {

    private final VkDevice device;
    private final long handle;

    private DescriptorLayout(VkDevice device, long handle) {
        this.device = device;
        this.handle = handle;
    }

    @SuppressWarnings("resource")
    public static DescriptorLayout create(VkDevice device, DescriptorLayoutSpec spec) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var bindings = VkDescriptorSetLayoutBinding.calloc(spec.bindings().size(), stack);

            for (int i = 0; i < spec.bindings().size(); i++) {
                DescriptorBindingSpec binding = spec.bindings().get(i);
                bindings.get(i)
                        .binding(binding.binding())
                        .descriptorType(binding.descriptorType())
                        .descriptorCount(binding.descriptorCount())
                        .stageFlags(binding.stageFlags())
                        .pImmutableSamplers(null);
            }

            var createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(bindings);

            var pDescriptorSetLayout = stack.mallocLong(1);
            VulkanUtils.crashIfFailure(
                    vkCreateDescriptorSetLayout(device, createInfo, null, pDescriptorSetLayout),
                    "Can't create descriptor set layout for SSBO(std430)"
            );

            return new DescriptorLayout(device, pDescriptorSetLayout.get(0));
        }
    }

    public long handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroyDescriptorSetLayout(device, handle, null);
    }
}
