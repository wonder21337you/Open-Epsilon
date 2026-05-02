package com.github.epsilon.mixins;

import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResourceLoadStateTracker.class)
public interface IResourceLoadStateTracker {

    @Accessor("reloadState")
    ResourceLoadStateTracker.ReloadState sakura$getReloadState();

}
