package com.github.lumin.mixins;

import com.github.lumin.modules.impl.render.Glow;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("RETURN"))
    private void onExtractRenderStateReturn(T entity, S state, float partialTick, CallbackInfo ci) {
        if (entity instanceof LivingEntity livingEntity) {
            if (Glow.INSTANCE.isEnabled() && Glow.INSTANCE.shouldRenderGlow(livingEntity)) {
                state.outlineColor = Glow.INSTANCE.getGlowColor(livingEntity);
            }
        }
    }
}