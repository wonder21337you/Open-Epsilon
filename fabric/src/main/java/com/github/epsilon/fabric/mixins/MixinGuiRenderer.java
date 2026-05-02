package com.github.epsilon.fabric.mixins;

import com.github.epsilon.fabric.utils.render.EpsilonMcGuiRenderer;
import com.github.epsilon.managers.ModuleManager;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {

    @Unique
    private GuiRenderState sakura$renderState;

    @Unique
    private EpsilonMcGuiRenderer sakura$guiRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(GuiRenderState renderState, MultiBufferSource.BufferSource bufferSource, SubmitNodeCollector submitNodeCollector, FeatureRenderDispatcher featureRenderDispatcher, List<PictureInPictureRenderer<?>> pictureInPictureRenderers, CallbackInfo ci) {
        if ((GuiRenderer) (Object) this instanceof EpsilonMcGuiRenderer) return;

        this.sakura$renderState = new GuiRenderState();

        this.sakura$guiRenderer = new EpsilonMcGuiRenderer(this.sakura$renderState, bufferSource, submitNodeCollector, featureRenderDispatcher, pictureInPictureRenderers);
    }

    @Inject(method = "draw", at = @At("HEAD"))
    private void onDrawHead(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        if ((GuiRenderer) (Object) this instanceof EpsilonMcGuiRenderer) return;

        Minecraft mc = Minecraft.getInstance();

        GuiGraphicsExtractor guiGraphics = new GuiGraphicsExtractor(mc, sakura$renderState, (int) mc.mouseHandler.getScaledXPos(mc.getWindow()), (int) mc.mouseHandler.getScaledYPos(mc.getWindow()));

        ModuleManager.INSTANCE.flushHuds(guiGraphics);

        sakura$guiRenderer.render(fogBuffer);

        sakura$guiRenderer.endFrame();
    }

}
