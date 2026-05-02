package com.github.epsilon.mixins;

import com.github.epsilon.Epsilon;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponentMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fixes a crash when connecting to servers (e.g. ViaProxy) that don't include
 * certain registry tags (e.g. {@code minecraft:damage_type / minecraft:is_fire})
 * in their configuration data.
 * <p>
 * The crash occurs because {@code Item$Properties.fireResistant()} uses
 * {@code context.getOrThrow(DamageTypeTags.IS_FIRE)} which throws
 * {@link IllegalStateException} when the tag is missing from the server's
 * registry data. This Mixin wraps each initializer's execution so that
 * a single missing tag doesn't crash the entire game.
 */
@Mixin(DataComponentInitializers.class)
public class MixinDataComponentInitializers {

    /**
     * Wraps the {@code InitializerEntry.run()} call inside
     * {@code runInitializers()} to gracefully handle missing registry
     * elements or tags sent by the server.
     */
    @WrapOperation(
            method = "runInitializers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/component/DataComponentInitializers$InitializerEntry;run(Lnet/minecraft/core/component/DataComponentMap$Builder;Lnet/minecraft/core/HolderLookup$Provider;)V"
            )
    )
    private void wrapInitializerRun(
            DataComponentInitializers.InitializerEntry instance,
            DataComponentMap.Builder components,
            HolderLookup.Provider context,
            Operation<Void> original
    ) {
        try {
            original.call(instance, components, context);
        } catch (Exception e) {
            Epsilon.LOGGER.warn(
                    "Skipping data component initializer due to missing registry data. "
                            + "This is normal when connecting to proxy servers (e.g. ViaProxy).",
                    e
            );
        }
    }

}
