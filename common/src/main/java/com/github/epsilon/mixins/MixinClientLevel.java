package com.github.epsilon.mixins;


import com.github.epsilon.Epsilon;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.class)
public class MixinClientLevel {

    @WrapOperation(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void hookTickNonPassenger(Entity instance, Operation<Void> original) {
        if (Epsilon.skipTicks > 0 && instance == Minecraft.getInstance().player) {
            Epsilon.skipTicks--;
        } else {
            original.call(instance);
        }
    }

}
