package com.github.epsilon.graphics.vulkan;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanQueue;
import org.lwjgl.vulkan.VkDevice;

import javax.annotation.Nullable;

public class LuminVulkanContext {

    private @Nullable VkDevice device;
    private long vma;

    // Queues
    private VulkanQueue graphicsQueue;
    private VulkanQueue computeQueue;
    private VulkanQueue transferQueue;

    public LuminVulkanContext() {
        if (!(RenderSystem.getDevice().backend instanceof VulkanDevice)) {
            return;
        }

        VulkanDevice blz3dDevice = (VulkanDevice) RenderSystem.getDevice().backend;

        this.device = blz3dDevice.vkDevice();
        this.vma = blz3dDevice.vma();

        this.graphicsQueue = blz3dDevice.graphicsQueue();
        this.computeQueue = blz3dDevice.computeQueue();
        this.transferQueue = blz3dDevice.transferQueue();
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

    public VulkanQueue graphicsQueue() {
        return this.graphicsQueue;
    }

    public VulkanQueue computeQueue() {
        return this.computeQueue;
    }

    public VulkanQueue transferQueue() {
        return this.transferQueue;
    }

}
