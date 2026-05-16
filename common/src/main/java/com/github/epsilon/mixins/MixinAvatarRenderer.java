package com.github.epsilon.mixins;

import com.github.epsilon.modules.impl.movement.ElytraFly;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static com.github.epsilon.Constants.mc;

@Mixin(AvatarRenderer.class)
public class MixinAvatarRenderer {

    @ModifyExpressionValue(method = "extractFlightData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Avatar;getFallFlyingTicks()I"))
    private int spoofFallFlyingTicks(int original, Avatar entity, AvatarRenderState reusedState, float partialTick) {
        if (ElytraFly.INSTANCE.isArmorMode() && entity == mc.player) {
            return 0;
        }
        return original;
    }

}
