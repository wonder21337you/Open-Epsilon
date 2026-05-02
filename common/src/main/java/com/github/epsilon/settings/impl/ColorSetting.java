package com.github.epsilon.settings.impl;

import com.github.epsilon.settings.Setting;

import java.awt.*;

public class ColorSetting extends Setting<Color> {

    private final boolean allowAlpha;

    public ColorSetting(String name, Color defaultValue, Dependency dependency, boolean allowAlpha) {
        super(name, dependency, null);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.allowAlpha = allowAlpha;
    }

    public boolean isAllowAlpha() {
        return allowAlpha;
    }

}