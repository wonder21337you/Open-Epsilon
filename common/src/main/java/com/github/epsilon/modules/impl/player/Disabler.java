package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.impl.PacketEvent;
import com.github.epsilon.mixins.IServerboundMovePlayerPacket;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

public class Disabler extends Module {

    public static final Disabler INSTANCE = new Disabler();

    private Disabler() {
        super("Disabler", Category.PLAYER);
    }


    private final BoolSetting badPacketsA = boolSetting("Bad Packets A", true);
    private final BoolSetting aimModulo360 = boolSetting("Aim Modulo 360", false);
    private final BoolSetting aimDuplicateLook = boolSetting("Aim Duplicate Look", false);

    private int lastSlot = -1;
    private float lastYaw, lastPitch;

    @EventHandler
    public void onPacket(PacketEvent.Send event) {
        if (badPacketsA.getValue()) {
            if (event.getPacket() instanceof ServerboundSetCarriedItemPacket packet) {
                int slot = packet.getSlot();
                if (slot == lastSlot && slot != -1) {
                    event.setCancelled(true);
                }
                lastSlot = packet.getSlot();
            }
        }

        if (aimModulo360.getValue()) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasRotation()) {
                IServerboundMovePlayerPacket accessor = (IServerboundMovePlayerPacket) packet;
                float yaw = accessor.getYRot();
                if (yaw < 360.0f && yaw > -360.0f) {
                    accessor.setYRot(yaw + 720.0f);
                }
                return;
            }
        }

        if (aimDuplicateLook.getValue()) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasRotation()) {
                IServerboundMovePlayerPacket accessor = (IServerboundMovePlayerPacket) packet;
                if (lastYaw == accessor.getYRot() && lastPitch == accessor.getXRot()) {
                    accessor.setYRot(accessor.getYRot() + 0.001f);
                }
                lastYaw = accessor.getYRot();
                lastPitch = accessor.getXRot();
            }
        }
    }

}
