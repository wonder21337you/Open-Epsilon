package com.github.epsilon.graphics.shaders;

import com.github.epsilon.assets.resources.ResourceLocationUtils;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.awt.*;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class BlurShader {

    public static final BlurShader INSTANCE = new BlurShader();

    private final Minecraft mc = Minecraft.getInstance();

    private static final Identifier identifier = ResourceLocationUtils.getIdentifier("blur");

    private static final int UNIFORMS_SIZE = new Std140SizeCalculator()
            .putVec3()
            .putVec4()
            .putVec4()
            .putVec4()
            .get();

    private RenderPipeline pipeline;
    private MappableRingBuffer uniforms;
    private RenderTarget input;

    private void ensureProgram() {
        if (this.uniforms == null) {
            this.uniforms = new MappableRingBuffer(() -> "LuminBlurUniforms", GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_UNIFORM, UNIFORMS_SIZE);
        }
        if (this.pipeline == null) {
            this.pipeline = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(ResourceLocationUtils.getIdentifier("pipeline/blur"))
                    .withVertexShader(identifier)
                    .withFragmentShader(identifier)
                    .withUniform("BlurUniforms", UniformType.UNIFORM_BUFFER)
                    .withSampler("InputSampler")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .build();
        }
    }

    private RenderTarget createRenderTarget() {
        return new RenderTarget("Lumin Blur Input", false) {
            @Override
            public void createBuffers(int width, int height) {
                RenderSystem.assertOnRenderThread();
                this.width = width;
                this.height = height;
                if (useDepth) {
                    this.depthTexture = RenderSystem.getDevice().createTexture(
                            () -> this.label + " / Depth",
                            GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                            TextureFormat.DEPTH32,
                            width, height, 1, 1
                    );
                    this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
                }
                this.colorTexture = RenderSystem.getDevice().createTexture(
                        () -> this.label + " / Color",
                        GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST,
                        TextureFormat.RGBA8,
                        width, height, 1, 1
                );
                this.colorTextureView = RenderSystem.getDevice().createTextureView(this.colorTexture);
            }
        };
    }

    public void render(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, Color color, float blurStrength) {
        this.ensureProgram();

        RenderTarget fb = mc.getMainRenderTarget();
        if (fb.getColorTexture() == null || fb.getColorTextureView() == null) {
            return;
        }

        int fbWidth = mc.getWindow().getWidth();
        int fbHeight = mc.getWindow().getHeight();

        if (this.input == null) {
            this.input = createRenderTarget();
            this.input.resize(fbWidth, fbHeight);
        } else if (this.input.width != fbWidth || this.input.height != fbHeight) {
            this.input.resize(fbWidth, fbHeight);
        }

        if (this.input.getColorTexture() == null || this.input.getColorTextureView() == null) {
            return;
        }

        float scale = (float) mc.getWindow().getGuiScale();
        float pxX = x * scale;
        float pxY = (-y + mc.getWindow().getGuiScaledHeight() - height) * scale;
        float pxW = width * scale;
        float pxH = height * scale;

        float rTLPx = Math.max(0.0f, rTL * scale);
        float rTRPx = Math.max(0.0f, rTR * scale);
        float rBRPx = Math.max(0.0f, rBR * scale);
        float rBLPx = Math.max(0.0f, rBL * scale);

        float quality = Math.max(0.0f, blurStrength);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(
                fb.getColorTexture(),
                input.getColorTexture(),
                0, 0, 0, 0, 0,
                fb.width, fb.height
        );

        try (GpuBuffer.MappedView view = encoder.mapBuffer(this.uniforms.currentBuffer(), false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putVec3(fb.width, fb.height, quality);
            builder.putVec4(pxW, pxH, pxX, pxY);
            builder.putVec4(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 1.0f);
            builder.putVec4(rTLPx, rTRPx, rBRPx, rBLPx);
        }

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "Lumin Blur",
                fb.getColorTextureView(),
                OptionalInt.empty(),
                fb.useDepth ? fb.getDepthTextureView() : null,
                OptionalDouble.empty())
        ) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("BlurUniforms", uniforms.currentBuffer());
            renderPass.bindTexture("InputSampler", input.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.draw(0, 3);
        }
        uniforms.rotate();
    }

    public void render(float x, float y, float width, float height, float radius, float blurStrength) {
        render(x, y, width, height, radius, radius, radius, radius, new Color(0, 0, 0, 0), blurStrength);
    }

    public void render(float x, float y, float width, float height, float radius, Color color, float blurStrength) {
        render(x, y, width, height, radius, radius, radius, radius, color, blurStrength);
    }

}
