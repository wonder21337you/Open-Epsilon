package com.github.epsilon.neoforge.utils.render;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;

import java.util.List;

public class EpsilonMcGuiRenderer extends GuiRenderer {

    public EpsilonMcGuiRenderer(GuiRenderState renderState, MultiBufferSource.BufferSource bufferSource, SubmitNodeCollector submitNodeCollector, FeatureRenderDispatcher featureRenderDispatcher, List<PictureInPictureRendererRegistration<?>> pipRendererFactories) {
        super(renderState, bufferSource, submitNodeCollector, featureRenderDispatcher, pipRendererFactories);
    }

}
