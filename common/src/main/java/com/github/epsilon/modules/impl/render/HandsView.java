package com.github.epsilon.modules.impl.render;

import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.impl.PlayerTickEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.IntSetting;

public class HandsView extends Module {

    public static final HandsView INSTANCE = new HandsView();

    private HandsView() {
        super("Hands View", Category.RENDER);
    }

    public enum SwingMode {
        Vanilla,
        Flux
    }

    private final BoolSetting disableSwapMain = boolSetting("Disable Swap Main", true);
    private final BoolSetting disableSwapOff = boolSetting("Disable Swap Off", true);

    public final EnumSetting<SwingMode> swingMode = enumSetting("Swing Mode", SwingMode.Vanilla);
    public final BoolSetting onlyWeapon = boolSetting("Only Weapon", true, () -> swingMode.is(SwingMode.Flux));

    public final IntSetting swingSpeed = intSetting("Swing Speed", 6, 0, 20, 1);

    public final BoolSetting swingWhileUsing = boolSetting("Visual Swing On Use", true);
    public final BoolSetting onlyOnBlock = boolSetting("Only On Block", true, swingWhileUsing::getValue);

    @EventHandler
    private void onPlayerTick(PlayerTickEvent.Pre event) {
        if (disableSwapMain.getValue() && mc.getEntityRenderDispatcher().getItemInHandRenderer().mainHandHeight <= 1f) {
            mc.getEntityRenderDispatcher().getItemInHandRenderer().mainHandHeight = 1f;
            mc.getEntityRenderDispatcher().getItemInHandRenderer().mainHandItem = mc.player.getMainHandItem();
        }

        if (disableSwapOff.getValue() && mc.getEntityRenderDispatcher().getItemInHandRenderer().offHandHeight <= 1f) {
            mc.getEntityRenderDispatcher().getItemInHandRenderer().offHandHeight = 1f;
            mc.getEntityRenderDispatcher().getItemInHandRenderer().offHandItem = mc.player.getOffhandItem();
        }
    }

}
