package com.github.epsilon.utils.client;

import com.github.epsilon.mixins.IMinecraft;
import com.github.epsilon.mixins.IReloadState;
import com.github.epsilon.mixins.IResourceLoadStateTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;

public class ClientUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean isLoading() {
        ResourceLoadStateTracker.ReloadState state = ((IResourceLoadStateTracker) ((IMinecraft) mc).sakura$getReloadStateTracker()).sakura$getReloadState();
        return state == null || !((IReloadState) state).sakura$isFinished();
    }

}
