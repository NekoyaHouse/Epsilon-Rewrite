package com.github.epsilon.graphics.vulkan;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import org.lwjgl.vulkan.VkDevice;

import javax.annotation.Nullable;

public class LuminVulkanContext {

    private @Nullable VkDevice device;
    private long vma;

    public LuminVulkanContext() {
        if (!(RenderSystem.getDevice().backend instanceof VulkanDevice)) {
            return;
        }

        VulkanDevice blz3dDevice = (VulkanDevice) RenderSystem.getDevice().backend;
        this.device = blz3dDevice.vkDevice();
        this.vma = blz3dDevice.vma();
    }

    public VkDevice device() {
        if (this.device == null) {
            throw new IllegalStateException("Vulkan device is not initialized. Make sure to initialize the Vulkan context properly.");
        }
        return this.device;
    }

    public long vma() {
        return this.vma;
    }

}
