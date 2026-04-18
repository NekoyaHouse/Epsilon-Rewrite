package com.github.epsilon.graphics.vulkan.buffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.Objects;

import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK12.*;

public final class VulkanStd430Buffer implements AutoCloseable {

    private final VulkanBuffer staging;
    private final VulkanBuffer gpu;
    private final Std430Writer writer;

    public VulkanStd430Buffer(long allocator, long sizeBytes, int gpuUsageFlags) {
        this.staging = VulkanBuffer.create(
                allocator,
                sizeBytes,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VMA_MEMORY_USAGE_CPU_TO_GPU,
                VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT | VMA_ALLOCATION_CREATE_MAPPED_BIT,
                true
        );

        this.gpu = VulkanBuffer.create(
                allocator,
                sizeBytes,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT | gpuUsageFlags,
                VMA_MEMORY_USAGE_GPU_ONLY,
                0,
                false
        );

        this.writer = new Std430Writer(staging.mappedData());
    }

    public static VulkanStd430Buffer storageBuffer(long allocator, long sizeBytes) {
        return new VulkanStd430Buffer(allocator, sizeBytes, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
    }

    public Std430Writer writer() {
        return writer;
    }

    public long gpuBuffer() {
        return gpu.handle();
    }

    public long stagingBuffer() {
        return staging.handle();
    }

    public long size() {
        return gpu.size();
    }

    public void map(VkCommandBuffer cmdBuf) {
        copy(cmdBuf, writer.writtenBytes());
    }

    public void copy(VkCommandBuffer cmdBuf) {
        copy(cmdBuf, writer.writtenBytes());
    }

    public void copy(VkCommandBuffer cmdBuf, long byteCount) {
        Objects.requireNonNull(cmdBuf, "cmdBuf");

        if (byteCount < 0 || byteCount > size()) {
            throw new IllegalArgumentException("byteCount out of range: " + byteCount);
        }

        staging.flush(0, byteCount);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCopy.Buffer regions = VkBufferCopy.calloc(1, stack)
                    .srcOffset(0)
                    .dstOffset(0)
                    .size(byteCount);

            vkCmdCopyBuffer(cmdBuf, staging.handle(), gpu.handle(), regions);
        }
    }

    @Override
    public void close() {
        gpu.close();
        staging.close();
    }
}

