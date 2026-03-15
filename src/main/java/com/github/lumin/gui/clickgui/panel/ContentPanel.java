package com.github.lumin.gui.clickgui.panel;

import com.github.lumin.graphics.shaders.BlurShader;
import com.github.lumin.gui.IComponent;
import com.github.lumin.gui.clickgui.component.impl.ColorSettingComponent;
import com.github.lumin.managers.ModuleManager;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.modules.impl.client.ClickGui;
import com.github.lumin.utils.render.MouseUtils;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ContentPanel implements IComponent {
    private final Minecraft mc = Minecraft.getInstance();

    private float x;
    private float y;
    private float width;
    private float height;
    private Category currentCategory;

    private final Animation viewAnimation = new Animation(Easing.EASE_OUT_EXPO, 450L);
    private final ListViewController listView = new ListViewController();
    private final SettingsViewController settingsView = new SettingsViewController();

    private float sourceCardX;
    private float sourceCardY;
    private float sourceCardW;
    private float sourceCardH;
    private boolean closeSettingsRequested;
    private boolean exitAnimationStarted;
    private ViewState currentState = ViewState.LIST;
    private ViewState targetState = ViewState.LIST;

    private enum ViewState {
        LIST,
        SETTINGS,
        OPENING_SETTINGS,
        CLOSING_SETTINGS
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setCurrentCategory(Category category) {
        if (this.currentCategory == category) return;
        this.currentCategory = category;
        closeSettingsRequested = false;
        settingsView.clearModule();
        currentState = ViewState.LIST;
        targetState = ViewState.LIST;
        viewAnimation.setStartValue(0.0f);
        List<Module> modules = new ArrayList<>();
        for (Module module : ModuleManager.INSTANCE.getModules()) {
            if (module.category == category) {
                modules.add(module);
            }
        }
        listView.setModules(modules);
    }

    @Override
    public void render(RendererSet set, int mouseX, int mouseY, float deltaTicks) {
        render(set, mouseX, mouseY, deltaTicks, 1.0f);
    }

    @Override
    public void render(RendererSet set, int mouseX, int mouseY, float deltaTicks, float alpha) {
        float guiScale = getGuiScale();
        float radius = guiScale * 20f;
        if (ClickGui.INSTANCE.isSidebarBlur()) {
            BlurShader.INSTANCE.drawBlur(x, y, width * guiScale, height * guiScale, 0, radius, radius, 0, ClickGui.INSTANCE.getBlurStrength());
        }
        set.bottomRoundRect().addRoundRect(x, y, width * guiScale, height * guiScale, 0, radius, radius, 0, new Color(0, 0, 0, 25));

        targetState = settingsView.hasActiveModule() && !closeSettingsRequested ? ViewState.SETTINGS : ViewState.LIST;
        if (currentState != targetState) {
            if (targetState == ViewState.SETTINGS) {
                currentState = ViewState.OPENING_SETTINGS;
                viewAnimation.setStartValue(0.0f);
            } else {
                currentState = ViewState.CLOSING_SETTINGS;
                viewAnimation.setStartValue(1.0f);
                exitAnimationStarted = false;
            }
        }

        if (currentState == ViewState.OPENING_SETTINGS) {
            viewAnimation.run(1.0f);
            if (viewAnimation.getValue() >= 0.99f) currentState = ViewState.SETTINGS;
            beginPanelClip(set, guiScale);
            renderListView(set, mouseX, mouseY, deltaTicks, alpha);
            settingsView.render(set, mouseX, mouseY, deltaTicks, alpha, x, y, width, height, guiScale);
            endPanelClip(set);
            return;
        }

        if (currentState == ViewState.CLOSING_SETTINGS) {
            closeSettingsRequested = true;
            beginPanelClip(set, guiScale);
            renderListView(set, mouseX, mouseY, deltaTicks, alpha);
            if (settingsView.hasActiveModule()) {
                if (!exitAnimationStarted) {
                    settingsView.startExitAnimation(sourceCardX, sourceCardY, sourceCardW, sourceCardH);
                    exitAnimationStarted = true;
                }
                settingsView.render(set, mouseX, mouseY, deltaTicks, alpha, x, y, width, height, guiScale);
                if (settingsView.isAnimationFinished()) {
                    currentState = ViewState.LIST;
                    settingsView.clearModule();
                    closeSettingsRequested = false;
                    exitAnimationStarted = false;
                }
            }
            endPanelClip(set);
            return;
        }

        if (currentState == ViewState.SETTINGS) {
            settingsView.render(set, mouseX, mouseY, deltaTicks, alpha, x, y, width, height, guiScale);
        } else {
            renderListView(set, mouseX, mouseY, deltaTicks, alpha);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        float guiScale = getGuiScale();
        if (isTransitioning()) return true;

        if (currentState == ViewState.SETTINGS && ColorSettingComponent.hasActivePicker()) {
            if (ColorSettingComponent.isMouseOutOfPicker((int) event.x(), (int) event.y())) {
                ColorSettingComponent.closeActivePicker();
                return true;
            }
            boolean handled = settingsView.mouseClicked(event, focused, x, y, width, height, guiScale);
            if (settingsView.consumeExitRequest()) closeSettingsRequested = true;
            return handled;
        }

        if (!MouseUtils.isHovering(x, y, width * guiScale, height * guiScale, event.x(), event.y())) {
            listView.clickOutside();
            settingsView.clickOutside();
            return false;
        }

        if (currentState == ViewState.SETTINGS) {
            boolean handled = settingsView.mouseClicked(event, focused, x, y, width, height, guiScale);
            if (settingsView.consumeExitRequest()) closeSettingsRequested = true;
            return handled;
        }

        if (currentState != ViewState.LIST) return false;
        boolean handled = listView.mouseClicked(event, focused, x, y, width, height, guiScale);
        ListViewController.OpenRequest openRequest = listView.consumeOpenRequest();
        if (openRequest == null) return handled;

        closeSettingsRequested = false;
        sourceCardX = openRequest.sourceX();
        sourceCardY = openRequest.sourceY();
        sourceCardW = openRequest.sourceW();
        sourceCardH = openRequest.sourceH();
        settingsView.openModule(openRequest.module(), sourceCardX, sourceCardY, sourceCardW, sourceCardH, x, y, width, height, guiScale);
        currentState = ViewState.OPENING_SETTINGS;
        viewAnimation.setStartValue(0.0f);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (isTransitioning()) return true;
        if (currentState == ViewState.SETTINGS)
            return settingsView.mouseReleased(event, x, y, width, height, getGuiScale());
        if (currentState == ViewState.LIST) return listView.mouseReleased(event, x, y, width, height, getGuiScale());
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isTransitioning()) return true;
        if (currentState == ViewState.SETTINGS) return settingsView.mouseScrolled(mouseX, mouseY, scrollY);
        if (currentState == ViewState.LIST) return listView.mouseScrolled(mouseX, mouseY, scrollY);
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isTransitioning()) return true;
        if (currentState == ViewState.SETTINGS) return settingsView.keyPressed(event);
        if (currentState == ViewState.LIST) return listView.keyPressed(event);
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (isTransitioning()) return true;
        if (currentState == ViewState.SETTINGS) return settingsView.charTyped(event);
        if (currentState == ViewState.LIST) return listView.charTyped(event);
        return false;
    }

    private void renderListView(RendererSet set, int mouseX, int mouseY, float deltaTicks, float alpha) {
        Module suppressModule = currentState == ViewState.LIST ? null : settingsView.getModule();
        listView.render(set, mouseX, mouseY, deltaTicks, alpha, x, y, width, height, getGuiScale(), suppressModule);
    }

    private boolean isTransitioning() {
        return currentState == ViewState.OPENING_SETTINGS || currentState == ViewState.CLOSING_SETTINGS;
    }

    private float getGuiScale() {
        return ClickGui.INSTANCE.scale.getValue().floatValue();
    }

    private void beginPanelClip(RendererSet set, float guiScale) {
        float clipX = x;
        float clipY = y;
        float clipW = width * guiScale;
        float clipH = height * guiScale;
        int fbW = mc.getWindow().getWidth();
        int fbH = mc.getWindow().getHeight();
        float pxScale = (float) mc.getWindow().getGuiScale();
        int scX = Mth.clamp(Mth.floor(clipX * pxScale), 0, fbW);
        int scY = Mth.clamp(Mth.floor((mc.getWindow().getGuiScaledHeight() - (clipY + clipH)) * pxScale), 0, fbH);
        int scW = Mth.clamp(Mth.ceil(clipW * pxScale), 0, fbW - scX);
        int scH = Mth.clamp(Mth.ceil(clipH * pxScale), 0, fbH - scY);
        set.bottomRoundRect().setScissor(scX, scY, scW, scH);
        set.topRoundRect().setScissor(scX, scY, scW, scH);
        set.font().setScissor(scX, scY, scW, scH);
    }

    private void endPanelClip(RendererSet set) {
        set.bottomRoundRect().clearScissor();
        set.topRoundRect().clearScissor();
        set.font().clearScissor();
    }
}
