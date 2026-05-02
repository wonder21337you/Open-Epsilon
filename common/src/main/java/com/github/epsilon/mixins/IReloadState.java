package com.github.epsilon.mixins;

import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResourceLoadStateTracker.ReloadState.class)
public interface IReloadState {

    @Accessor("finished")
    boolean sakura$isFinished();

}
