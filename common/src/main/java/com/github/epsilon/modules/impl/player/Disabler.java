package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.impl.PacketEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.utils.player.ChatUtils;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

public class Disabler extends Module {

    public static final Disabler INSTANCE = new Disabler();

    private Disabler() {
        super("Disabler", Category.PLAYER);
    }

    private final BoolSetting logging = boolSetting("Logging", false);
    private final BoolSetting badPacketsA = boolSetting("BadPacketsA", true);
    private final BoolSetting aimModulo360 = boolSetting("AimModulo360", false);

    private int lastSlot = -1;

    @EventHandler
    private void onPacket(PacketEvent.Send event) {
        if (badPacketsA.getValue()) {
            if (event.getPacket() instanceof ServerboundSetCarriedItemPacket packet) {
                int slot = packet.getSlot();
                if (slot == lastSlot && slot != -1) {
                    event.setCancelled(true);
                    log("Disabled BadPacketsA");
                }
                lastSlot = packet.getSlot();
                return;
            }
        }

        if (aimModulo360.getValue()) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasRotation()) {
                float yRot = packet.yRot;
                if (yRot < 360.0f && yRot > -360.0f) {
                    packet.yRot = yRot + 720.0f;
                    log("Disabled AimModulo360");
                }
            }
        }
    }

    private void log(String message) {
        if (logging.getValue()) ChatUtils.addChatMessage("[Disabler] " + message);
    }

}
