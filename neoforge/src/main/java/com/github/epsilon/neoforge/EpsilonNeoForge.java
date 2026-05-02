package com.github.epsilon.neoforge;

import com.github.epsilon.Epsilon;
import com.github.epsilon.addon.AddonBootstrap;
import com.github.epsilon.assets.i18n.LanguageReloadListener;
import com.github.epsilon.assets.resources.ResourceLocationUtils;
import com.github.epsilon.neoforge.addon.EpsilonAddonSetupEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = Epsilon.MOD_ID, value = Dist.CLIENT)
public class EpsilonNeoForge {

    public static void init() {
        EpsilonAddonSetupEvent addonEvent = NeoForge.EVENT_BUS.post(new EpsilonAddonSetupEvent());
        AddonBootstrap.registerAddons(addonEvent.getAddons());

        Epsilon.init();
    }

    @SubscribeEvent
    private static void onResourcesReload(AddClientReloadListenersEvent event) {
        event.addListener(ResourceLocationUtils.getIdentifier("objects/reload_listener"), new LanguageReloadListener());
    }

}
