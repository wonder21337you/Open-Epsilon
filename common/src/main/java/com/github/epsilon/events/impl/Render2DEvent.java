package com.github.epsilon.events.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Render2DEvent {

    private final GuiGraphicsExtractor guiGraphics;

    public Render2DEvent(GuiGraphicsExtractor guiGraphics) {
        this.guiGraphics = guiGraphics;
    }

    public GuiGraphicsExtractor getGuiGraphics() {
        return guiGraphics;
    }

}
