package com.github.epsilon.managers.sound;

import com.github.epsilon.assets.resources.ResourceLocationUtils;
import net.minecraft.resources.Identifier;

public enum SoundKey {

    ENABLE("enable"),
    DISABLE("disable"),
    SETTINGS_OPEN("settings_open"),
    SETTINGS_CLOSE("settings_close"),
    SHUTDOWN("shutdown");

    private final String path;

    SoundKey(String path) {
        this.path = path;
    }

    public Identifier id() {
        return ResourceLocationUtils.getIdentifier(path);
    }

}
