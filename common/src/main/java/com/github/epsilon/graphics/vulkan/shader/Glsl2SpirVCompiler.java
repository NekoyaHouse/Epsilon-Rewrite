package com.github.epsilon.graphics.vulkan.shader;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.util.shaderc.Shaderc.*;

public final class Glsl2SpirVCompiler implements AutoCloseable {

    private final String glslShaderSource;
    private final int shaderKind;
    private final String sourceName;
    private final String entryPoint;

    private final long compiler;
    private final long options;
    private long result;

    public Glsl2SpirVCompiler(String glslShaderSource) {
        this(glslShaderSource, shaderc_glsl_compute_shader, "inline.comp", "main");
    }

    public Glsl2SpirVCompiler(String glslShaderSource, int shaderKind, String sourceName, String entryPoint) {
        this.glslShaderSource = Objects.requireNonNull(glslShaderSource, "glslShaderSource");
        this.shaderKind = shaderKind;
        this.sourceName = Objects.requireNonNull(sourceName, "sourceName");
        this.entryPoint = Objects.requireNonNull(entryPoint, "entryPoint");

        this.compiler = shaderc_compiler_initialize();
        if (this.compiler == 0L) {
            throw new IllegalStateException("Failed to initialize shaderc compiler");
        }

        this.options = shaderc_compile_options_initialize();
        if (this.options == 0L) {
            shaderc_compiler_release(this.compiler);
            throw new IllegalStateException("Failed to initialize shaderc compile options");
        }

        shaderc_compile_options_set_target_env(this.options, shaderc_target_env_vulkan, shaderc_env_version_vulkan_1_2);
        shaderc_compile_options_set_target_spirv(this.options, shaderc_spirv_version_1_5);
    }

    public void compile() {
        if (result != 0L) {
            shaderc_result_release(result);
            result = 0L;
        }

        result = shaderc_compile_into_spv(
                compiler,
                glslShaderSource,
                shaderKind,
                sourceName,
                entryPoint,
                options
        );

        if (result == 0L) {
            throw new IllegalStateException("shaderc returned null result handle");
        }

        int status = shaderc_result_get_compilation_status(result);
        if (status != shaderc_compilation_status_success) {
            String error = shaderc_result_get_error_message(result);
            throw new IllegalStateException("Shader compilation failed: " + error);
        }
    }

    public ByteBuffer getSpirV() {
        if (result == 0L) {
            throw new IllegalStateException("Shader is not compiled. Call compile() first.");
        }

        ByteBuffer compiled = shaderc_result_get_bytes(result);
        if (compiled == null) {
            throw new IllegalStateException("shaderc returned empty SPIR-V data");
        }
        ByteBuffer copy = BufferUtils.createByteBuffer(compiled.remaining());
        copy.put(compiled.duplicate());
        copy.flip();
        return copy;
    }

    @Override
    public void close() {
        if (result != 0L) {
            shaderc_result_release(result);
            result = 0L;
        }
        shaderc_compile_options_release(options);
        shaderc_compiler_release(compiler);
    }
}
