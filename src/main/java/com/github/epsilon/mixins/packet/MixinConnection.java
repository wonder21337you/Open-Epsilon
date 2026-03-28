package com.github.epsilon.mixins.packet;

import com.github.epsilon.events.PacketEvent;
import com.github.epsilon.utils.network.PacketUtils;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Connection.class)
public class MixinConnection {

    @Shadow
    public void send(Packet<?> packet, @Nullable ChannelFutureListener sendListener) {
    }

    @ModifyVariable(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> onReceivePacket(Packet<?> packet) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            for (Packet<? super ClientGamePacketListener> subPacket : bundle.subPackets()) {
                PacketEvent.Receive receive = NeoForge.EVENT_BUS.post(new PacketEvent.Receive(subPacket));
                if (!receive.isCanceled()) {
                    packets.add((Packet<? super ClientGamePacketListener>) receive.getPacket());
                }
            }
            return new ClientboundBundlePacket(packets);
        }

        PacketEvent.Receive receive = NeoForge.EVENT_BUS.post(new PacketEvent.Receive(packet));
        if (receive.isCanceled()) {
            return null;
        }

        return receive.getPacket();
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, @Nullable ChannelFutureListener sendListener, CallbackInfo ci) {
        if (PacketUtils.bypassPackets.contains(packet)) {
            PacketUtils.bypassPackets.remove(packet);
            send(packet, sendListener);
        } else {
            PacketEvent.Send sendEvent = new PacketEvent.Send(packet);
            if (NeoForge.EVENT_BUS.post(sendEvent).isCanceled()) {
                ci.cancel();
                return;
            }
            /*
              给event.setPacket后的包添加到bypassPackets队列里发出去，不然Minecraft还是会发原来的包出去
             */
            if (sendEvent.getPacket() != packet) {
                ci.cancel();
                PacketUtils.bypassPackets.add(sendEvent.getPacket());
                send(sendEvent.getPacket(), sendListener);
            }
        }
    }

}
