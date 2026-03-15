package com.github.lumin.modules.impl.combat;

import com.github.lumin.assets.resources.ResourceLocationUtils;
import com.github.lumin.managers.RotationManager;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.*;
import com.github.lumin.utils.math.MathUtils;
import com.github.lumin.utils.render.ColorUtils;
import com.github.lumin.utils.rotation.MovementFix;
import com.github.lumin.utils.rotation.Priority;
import com.github.lumin.utils.rotation.RotationUtils;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class KillAura extends Module {

    public static final KillAura INSTANCE = new KillAura();

    private KillAura() {
        super("KillAura", Category.COMBAT);
    }

    public enum TargetMode {
        Single,
        Switch,
        Multiple,
    }

    private enum ESPMode {
        CaptureMark,
        Firefly
    }

    private final EnumSetting<TargetMode> targetMode = enumSetting("TargetMode", TargetMode.Single);
    private final DoubleSetting range = doubleSetting("Range", 3.0, 1.0, 6.0, 0.01);
    private final DoubleSetting aimRange = doubleSetting("AimRange", 4.0, 1.0, 6.0, 0.1);
    private final IntSetting rotationSpeed = intSetting("roationspeed", 10, 1, 10, 1);
    private final DoubleSetting fov = doubleSetting("FOV", 360.0, 10.0, 360.0, 1.0);
    private final BoolSetting cooldownATK = boolSetting("CooldownATK", false);
    private final DoubleSetting cps = doubleSetting("CPS", 10.0, 1.0, 20.0, 1.0);
    private final DoubleSetting maxCps = doubleSetting("MaxCPS", 12, 1, 20, 1);
    private final BoolSetting player = boolSetting("Player", true);
    private final BoolSetting mob = boolSetting("Mob", true);
    private final BoolSetting animal = boolSetting("Animal", true);
    private final BoolSetting Invisible = boolSetting("Invisible", true);
    private final BoolSetting esp = boolSetting("ESP", false);
    private final EnumSetting<ESPMode> espMode = enumSetting("ESPMode", ESPMode.Firefly, esp::getValue);
    private final ColorSetting espColor1 = colorSetting("ESPMain", new Color(255, 183, 197), esp::getValue);
    private final ColorSetting espColor2 = colorSetting("ESPSecond", new Color(255, 133, 161), esp::getValue);
    private final DoubleSetting espSize = doubleSetting("ESPSize", 1.2, 0.5, 3.0, 0.1, esp::getValue);
    private final DoubleSetting espRotSpeed = doubleSetting("RotSpeed", 2.0, 0.5, 10.0, 0.1, esp::getValue);
    private final DoubleSetting waveSpeed = doubleSetting("WaveSpeed", 3.0, 0.5, 10.0, 0.1, esp::getValue);

    private LivingEntity target;
    private final List<LivingEntity> targets = new ArrayList<>();

    private int switchIndex = 0;
    private float attacks = 0;

    private float rotation = 0f;

    private static final Identifier TARGET_TEX = ResourceLocationUtils.getIdentifier("textures/particles/target.png");

    private final RenderPipeline TARGET_ICON_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation("pipeline/sakura_target_icon")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build();

    private final Function<Identifier, RenderType> TARGET_ICON_LAYER = Util.memoize(texture -> RenderType.create(
            "sakura_target_icon",
            RenderSetup.builder(TARGET_ICON_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .sortOnUpload()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.MAIN_TARGET)
                    .createRenderSetup()
    ));

    @Override
    protected void onDisable() {
        target = null;
        targets.clear();
        switchIndex = 0;
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre e) {
        if (nullCheck()) return;

        targets.clear();
        updateTargets();

        if (targets.isEmpty()) {
            target = null;
            return;
        }

        if (targetMode.is("Single")) {
            target = targets.getFirst();
        } else if (targetMode.is("Switch")) {
            if (switchIndex >= targets.size()) switchIndex = 0;
            target = targets.get(switchIndex);
        } else if (targetMode.is("Multi")) {
            target = targets.getFirst();
        }

        attacks += MathUtils.getRandom(cps.getValue().floatValue(), maxCps.getValue().floatValue()) / 20f;

        if (target != null) {
            float[] rotations = RotationUtils.getRotationsToEntity(target);
            RotationManager.INSTANCE.setRotations(new Vector2f(rotations[0], rotations[1]), rotationSpeed.getValue().floatValue(), MovementFix.ON, Priority.Medium);
        }
    }

    @SubscribeEvent
    public void onClick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        if (target == null) return;
        if (mc.player.isUsingItem() || mc.player.isBlocking()) return;
        if (mc.player.getAttackStrengthScale(0.5f) < 1.0f && cooldownATK.getValue()) return;

        while (attacks >= 1) {
            if (targetMode.is("Multi")) {
                for (LivingEntity t : targets) {
                    if (RotationUtils.getEyeDistanceToEntity(t) <= range.getValue() && mc.hitResult.getType() == HitResult.Type.ENTITY) {
                        doAttack();
                    }
                }
                switchIndex++;
            } else {
                if (RotationUtils.getEyeDistanceToEntity(target) <= range.getValue() && mc.hitResult.getType() == HitResult.Type.ENTITY && mc.crosshairPickEntity.is(target)) {
                    doAttack();
                    if (targetMode.is("Switch")) switchIndex++;
                } else if (targetMode.is("Switch")) {
                    switchIndex++;
                }
            }
            attacks -= 1;
        }
    }

    @SubscribeEvent
    private void onRender3D(RenderLevelStageEvent.AfterEntities event) {
        if (nullCheck() || !esp.getValue() || target == null) return;

        float deltaTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        switch (espMode.getValue()) {
            case CaptureMark -> renderEsp(target, event.getPoseStack(), deltaTick);
        }

    }

    private void doAttack() {
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void updateTargets() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.isDeadOrDying()) continue;
            if (AntiBot.INSTANCE.isBot(entity)) continue;

            double dist = RotationUtils.getEyeDistanceToEntity(living);
            if (dist > aimRange.getValue()) continue;

            if (!isValidTarget(living)) continue;
            if (!RotationUtils.isInFov(living, fov.getValue().floatValue())) continue;
            targets.sort(Comparator.comparingDouble(o -> (double) o.distanceTo(mc.player)));
            targets.add(living);
        }
        targets.sort(Comparator.comparingDouble(RotationUtils::getEyeDistanceToEntity));
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof Player) {
            if (!player.getValue()) return false;
            return !entity.isInvisible() || Invisible.getValue();
        } else if (entity instanceof Animal || entity instanceof Villager) {
            return animal.getValue();
        } else if (entity instanceof Monster) {
            return mob.getValue();
        } else {
            return false;
        }
    }

    private void renderEsp(LivingEntity target, PoseStack poseStack, float tickDelta) {
        rotation -= espRotSpeed.getValue().floatValue();
        if (rotation <= -360f) rotation += 360f;

        Vec3 cam = mc.getEntityRenderDispatcher().camera.position();

        double ex = Mth.lerp(tickDelta, target.xOld, target.getX()) - cam.x;
        double ey = Mth.lerp(tickDelta, target.yOld, target.getY()) - cam.y;
        double ez = Mth.lerp(tickDelta, target.zOld, target.getZ()) - cam.z;

        float entityHeight = target.getBbHeight();
        float size = espSize.getValue().floatValue() * 0.5f;

        poseStack.pushPose();
        poseStack.translate(ex, ey + entityHeight * 0.5, ez);

        Camera camera = mc.gameRenderer.getMainCamera();
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));

        drawTextureQuad(poseStack, size);

        poseStack.popPose();
    }

    private void drawTextureQuad(PoseStack matrices, float size) {
        Matrix4f matrix = matrices.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        Color c1 = getColorForProgress(0);
        Color c2 = getColorForProgress(0.25f);
        Color c3 = getColorForProgress(0.5f);
        Color c4 = getColorForProgress(0.75f);

        buffer.addVertex(matrix, -size, -size, 0).setUv(0, 0).setColor(c1.getRGB());
        buffer.addVertex(matrix, -size, size, 0).setUv(0, 1).setColor(c2.getRGB());
        buffer.addVertex(matrix, size, size, 0).setUv(1, 1).setColor(c3.getRGB());
        buffer.addVertex(matrix, size, -size, 0).setUv(1, 0).setColor(c4.getRGB());

        TARGET_ICON_LAYER.apply(TARGET_TEX).draw(buffer.buildOrThrow());
    }

    private Color getColorForProgress(float progress) {
        float wave = (float) Math.sin((progress * Math.PI * 2) + (System.currentTimeMillis() / 1000f * waveSpeed.getValue()));
        wave = (wave + 1f) / 2f;
        return ColorUtils.interpolateColor(espColor1.getValue(), espColor2.getValue(), wave);
    }

}