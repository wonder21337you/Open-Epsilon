package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.managers.TargetManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.*;
import com.github.epsilon.utils.combat.DamageUtils;
import com.github.epsilon.utils.player.FindItemResult;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.render.Render3DUtils;
import com.github.epsilon.utils.render.WorldToScreen;
import com.github.epsilon.utils.rotation.Priority;
import com.github.epsilon.utils.rotation.RaytraceUtils;
import com.github.epsilon.utils.rotation.RotationUtils;
import com.github.epsilon.utils.timer.TimerUtils;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CrystalAura extends Module {

    public static final CrystalAura INSTANCE = new CrystalAura();

    private CrystalAura() {
        super("CrystalAura", Category.COMBAT);
    }

    // General
    private final DoubleSetting targetRange = doubleSetting("Target Range", 6.0, 0.0, 12.0, 0.5);
    private final EnumSetting<AimOptimizeMode> aimOptimizeMode = enumSetting("Aim Optimize Mode", AimOptimizeMode.MaxDamage);
    private final DoubleSetting aimDamageSacrifice = doubleSetting("Aim Damage Sacrifice", 1.0, 0.0, 10.0, 0.25,
            () -> aimOptimizeMode.is(AimOptimizeMode.LowRotation));
    private final BoolSetting eatingPause = boolSetting("Eating Pause", false);

    // Calculation
    private final BoolSetting noSuicide = boolSetting("No Suicide", true);
    private final DoubleSetting lethalMaxSelfDamage = doubleSetting("Lethal Max Self Dmg", 8.0, 0.0, 36.0, 0.25);
    private final BoolSetting motionPrediction = boolSetting("Motion Prediction", false);
    private final IntSetting predictTick = intSetting("Predict Tick", 6, 0, 10, 1, motionPrediction::getValue);
    private final EnumSetting<DamageUtils.ArmorEnchantmentMode> armorMode = enumSetting("Armor Mode", DamageUtils.ArmorEnchantmentMode.PPBP);
    private final BoolSetting armorForSelf = boolSetting("Armor Mode For Self", false, () -> !armorMode.is(DamageUtils.ArmorEnchantmentMode.None));

    // Force Place
    private final DoubleSetting forcePlaceHealth = doubleSetting("Force Place Health", 8.0, 0.0, 36.0, 0.5);
    private final IntSetting forcePlaceArmorRate = intSetting("Force Place Armor Rate", 3, 0, 25, 1);
    private final DoubleSetting forcePlaceMinDamage = doubleSetting("Force Place Min Dmg", 2.0, 0.0, 20.0, 0.25);
    private final DoubleSetting forcePlaceBalance = doubleSetting("Force Place Balance", -3.0, -10.0, 10.0, 0.25);

    // Place
    private final EnumSetting<SwingMode> placeSwing = enumSetting("Place Swing", SwingMode.None);
    private final DoubleSetting placeRotationSpeed = doubleSetting("Place Rotation Speed", 10.0, 1.0, 10.0, 0.5);
    private final EnumSetting<SwapMode> placeSwapMode = enumSetting("Place Swap Mode", SwapMode.None);
    private final DoubleSetting placeMinDmg = doubleSetting("Place Min Dmg", 6.0, 0.0, 20.0, 0.25);
    private final DoubleSetting placeMaxSelfDmg = doubleSetting("Place Max Self Dmg", 10.0, 0.0, 36.0, 0.25);
    private final DoubleSetting placeBalance = doubleSetting("Place Balance", -3.0, -10.0, 10.0, 0.25);
    private final IntSetting placeDelay = intSetting("Place Delay", 50, 0, 1000, 10);
    private final DoubleSetting placeRange = doubleSetting("Place Range", 4.0, 1.0, 6.0, 0.1);
    private final DoubleSetting placeWallRange = doubleSetting("Place Wall Range", 3.0, 0.0, 6.0, 0.1);

    // Break
    private final EnumSetting<SwingMode> breakSwing = enumSetting("Break Swing", SwingMode.None);
    private final DoubleSetting breakRotationSpeed = doubleSetting("Break Rotation Speed", 10.0, 1.0, 10.0, 0.5);
    private final BoolSetting antiWeak = boolSetting("Anti Weak", false);
    private final EnumSetting<SwapMode> antiWeakSwapMode = enumSetting("Anti Weak Swap Mode", SwapMode.Silent, antiWeak::getValue);
    private final DoubleSetting breakMinDmg = doubleSetting("Break Min Dmg", 6.0, 0.0, 20.0, 0.25);
    private final DoubleSetting breakMaxSelfDmg = doubleSetting("Break Max Self Dmg", 10.0, 0.0, 36.0, 0.25);
    private final DoubleSetting breakBalance = doubleSetting("Break Balance", -3.0, -10.0, 10.0, 0.25);
    private final IntSetting breakDelay = intSetting("Break Delay", 50, 0, 1000, 10);
    private final DoubleSetting breakRange = doubleSetting("Break Range", 4.0, 1.0, 6.0, 0.1);

    // Render
    private final BoolSetting showDamage = boolSetting("Show Damage", false);
    private final ColorSetting filledColor = colorSetting("Filled Color", new Color(255, 150, 120, 100));
    private final ColorSetting outlineColor = colorSetting("Outline Color", new Color(255, 150, 120, 170));
    private final DoubleSetting outlineWidth = doubleSetting("Outline Width", 3.0, 1.0, 10.0, 0.5);
    private final IntSetting movingLength = intSetting("Moving Length", 400, 0, 1000, 50);
    private final IntSetting fadeLength = intSetting("Fade Length", 200, 0, 1000, 50);

    private LivingEntity target;
    private final TimerUtils placeTimer = new TimerUtils();
    private final TimerUtils breakTimer = new TimerUtils();
    private final TimerUtils renderTimer = new TimerUtils();
    private final List<RenderRecord> renderRecords = new ArrayList<>();

    private final Supplier<TextRenderer> rectRenderer = Suppliers.memoize(() -> new TextRenderer(128 * 1024));

    @Override
    protected void onEnable() {
        placeTimer.reset();
        breakTimer.reset();
        renderTimer.reset();
        renderRecords.clear();
    }

    @Override
    protected void onDisable() {
        target = null;
        renderRecords.clear();
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        if (eatingPause.getValue() && mc.player.isUsingItem()) return;

        target = TargetManager.INSTANCE.acquirePrimary(
                TargetManager.TargetRequest.of(
                        targetRange.getValue(),
                        360.0f,
                        true,
                        false,
                        false,
                        false,
                        true,
                        1
                )
        );

        if (target == null || !target.isAlive()) {
            return;
        }

        Vec3 predictedPos = getPredictedTargetPos(target);

        tryBreakCrystal();
        tryPlaceCrystal(predictedPos);
    }

    private void tryBreakCrystal() {
        if (!breakTimer.passedMillise(breakDelay.getValue())) return;

        List<BreakCandidate> candidates = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (!crystal.isAlive()) continue;

            double distToPlayer = mc.player.distanceTo(crystal);
            if (distToPlayer > breakRange.getValue()) continue;

            Vec3 crystalPos = crystal.position();

            float targetDmg = DamageUtils.crystalDamage(target, crystalPos, armorMode.getValue());
            float selfDmg = DamageUtils.selfCrystalDamage(crystalPos, armorForSelf.getValue() ? armorMode.getValue() : DamageUtils.ArmorEnchantmentMode.None);
            if (exceedsSelfDamageLimit(selfDmg, breakMaxSelfDmg.getValue())) continue;
            if (targetDmg < breakMinDmg.getValue()) continue;
            float balance = targetDmg - selfDmg;
            if (balance < breakBalance.getValue()) continue;

            Vector2f targetRotation = RotationUtils.calculate(crystal);
            float rotationDelta = getRotationDelta(getPrevTickRotation(), targetRotation);
            candidates.add(new BreakCandidate(crystal, targetDmg, targetRotation, rotationDelta));
        }

        EndCrystal bestCrystal = selectBestBreakCandidate(candidates);
        if (bestCrystal != null) {
            doBreakCrystal(bestCrystal);
        }
    }

    private void doBreakCrystal(EndCrystal crystal) {
        boolean swapped = false;
        if (antiWeak.getValue() && mc.player.hasEffect(MobEffects.WEAKNESS)) {
            FindItemResult sword = InvUtils.findInHotbar(Items.DIAMOND_SWORD, Items.NETHERITE_SWORD, Items.IRON_SWORD, Items.STONE_SWORD);
            if (sword.found()) {
                switch (antiWeakSwapMode.getValue()) {
                    case Swap -> InvUtils.swap(sword.slot(), false);
                    case Silent -> swapped = InvUtils.swap(sword.slot(), true);
                    default -> {
                    }
                }
            }
        }

        final boolean silentSwapped = swapped;
        final int requestPriority = Priority.High.priority;
        final int crystalId = crystal.getId();
        final Vector2f rotation = RotationUtils.calculate(crystal);

        RotationManager.INSTANCE.applyRotation(rotation, breakRotationSpeed.getValue(), requestPriority, record -> {
            if (!isEnabled() || nullCheck()) {
                if (silentSwapped) InvUtils.swapBack();
                return;
            }
            if (record.selectedPriorityValue() != requestPriority) return;

            Entity current = mc.level.getEntity(crystalId);
            if (!(current instanceof EndCrystal currentCrystal) || !currentCrystal.isAlive()) {
                if (silentSwapped) InvUtils.swapBack();
                return;
            }

            if (!RaytraceUtils.facingEnemy(currentCrystal, breakRange.getValue(), record.currentRotation())) {
                if (silentSwapped) InvUtils.swapBack();
                return;
            }

            mc.gameMode.attack(mc.player, currentCrystal);
            doSwing(breakSwing.getValue());
            breakTimer.reset();
            float renderDamage = DamageUtils.crystalDamage(target, currentCrystal.position(), armorMode.getValue());
            addRenderRecord(currentCrystal.blockPosition().below(), renderDamage);

            if (silentSwapped) InvUtils.swapBack();
        });
    }

    private void tryPlaceCrystal(Vec3 predictedTargetPos) {
        if (!placeTimer.passedMillise(placeDelay.getValue())) return;

        FindItemResult crystalItem = findCrystalItem();
        if (!crystalItem.found()) return;
        List<PlaceCandidate> candidates = collectPlaceCandidates(predictedTargetPos);

        PlaceCandidate bestNormal = findBestCandidate(candidates,
                placeMinDmg.getValue().floatValue(),
                placeMaxSelfDmg.getValue().floatValue(),
                placeBalance.getValue().floatValue());

        if (bestNormal != null) {
            doPlaceCrystal(bestNormal, crystalItem);
            return;
        }

        if (shouldForcePlace()) {
            PlaceCandidate bestForce = findBestCandidate(candidates,
                    forcePlaceMinDamage.getValue().floatValue(),
                    placeMaxSelfDmg.getValue().floatValue(),
                    forcePlaceBalance.getValue().floatValue());

            if (bestForce != null) {
                doPlaceCrystal(bestForce, crystalItem);
            }
        }
    }

    /**
     * Checks whether Force Place conditions are met:
     * - target health ≤ forcePlaceHealth  OR
     * - target armor durability rate ≤ forcePlaceArmorRate (total armor value)
     */
    private boolean shouldForcePlace() {
        if (target.getHealth() <= forcePlaceHealth.getValue()) return true;

        float targetArmor = (float) target.getAttributeValue(Attributes.ARMOR);
        return targetArmor <= forcePlaceArmorRate.getValue();
    }

    private List<PlaceCandidate> collectPlaceCandidates(Vec3 predictedTargetPos) {
        List<PlaceCandidate> candidates = new ArrayList<>();

        BlockPos center = BlockPos.containing(predictedTargetPos);
        int range = 5;
        double placeRangeSq = placeRange.getValue() * placeRange.getValue();
        double wallRangeSq = placeWallRange.getValue() * placeWallRange.getValue();
        Vec3 playerEye = mc.player.getEyePosition();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos supportPos = center.offset(x, y, z);

                    BlockState supportState = mc.level.getBlockState(supportPos);
                    if (!supportState.is(Blocks.OBSIDIAN) && !supportState.is(Blocks.BEDROCK)) continue;
                    BlockPos crystalBlockPos = supportPos.above();
                    if (!mc.level.getBlockState(crystalBlockPos).isAir()) continue;
                    if (!mc.level.getBlockState(crystalBlockPos.above()).isAir()) continue;
                    AABB crystalBB = new AABB(crystalBlockPos);
                    if (!mc.level.getEntities(null, crystalBB).isEmpty()) continue;
                    Vec3 supportCenter = Vec3.atCenterOf(supportPos);
                    double distanceSq = playerEye.distanceToSqr(supportCenter);
                    if (distanceSq > placeRangeSq) continue;
                    Vec3 crystalPos = new Vec3(
                            supportPos.getX() + 0.5,
                            supportPos.getY() + 1.0,
                            supportPos.getZ() + 0.5
                    );

                    float targetDmg = DamageUtils.crystalDamage(target, crystalPos, armorMode.getValue());
                    float selfDmg = DamageUtils.selfCrystalDamage(crystalPos, armorForSelf.getValue() ? armorMode.getValue() : DamageUtils.ArmorEnchantmentMode.None);
                    Vector2f targetRotation = RotationUtils.calculate(supportPos, Direction.UP);
                    boolean visible = RaytraceUtils.overBlock(targetRotation, Direction.UP, supportPos, false);
                    boolean wallBypassAllowed = !visible && distanceSq <= wallRangeSq;
                    if (!visible && !wallBypassAllowed) continue;

                    candidates.add(new PlaceCandidate(
                            supportPos,
                            crystalPos,
                            targetDmg,
                            selfDmg,
                            targetRotation,
                            !visible,
                            wallBypassAllowed
                    ));
                }
            }
        }

        return candidates;
    }

    private PlaceCandidate findBestCandidate(List<PlaceCandidate> candidates,
                                             float minDmg, float maxSelfDmg, float minBalance) {
        List<PlaceCandidate> visibleValid = new ArrayList<>();
        List<PlaceCandidate> wallValid = new ArrayList<>();

        for (PlaceCandidate c : candidates) {
            if (exceedsSelfDamageLimit(c.selfDmg, maxSelfDmg)) continue;
            if (c.targetDmg < minDmg) continue;
            float balance = c.targetDmg - c.selfDmg;
            if (balance < minBalance) continue;
            if (c.throughWall) {
                if (c.wallBypassAllowed) {
                    wallValid.add(c);
                }
            } else {
                visibleValid.add(c);
            }
        }

        List<PlaceCandidate> valid = !visibleValid.isEmpty() ? visibleValid : wallValid;

        if (valid.isEmpty()) return null;

        float bestDamage = Float.MIN_VALUE;
        for (PlaceCandidate c : valid) {
            if (c.targetDmg > bestDamage) {
                bestDamage = c.targetDmg;
            }
        }

        if (aimOptimizeMode.is(AimOptimizeMode.MaxDamage)) {
            PlaceCandidate best = null;
            float bestScore = Float.MIN_VALUE;
            for (PlaceCandidate c : valid) {
                if (c.targetDmg > bestScore) {
                    bestScore = c.targetDmg;
                    best = c;
                }
            }
            return best;
        }

        float threshold = bestDamage - aimDamageSacrifice.getValue().floatValue();
        PlaceCandidate selected = null;
        float bestDelta = Float.MAX_VALUE;
        Vector2f prev = getPrevTickRotation();
        for (PlaceCandidate c : valid) {
            if (c.targetDmg < threshold) continue;
            float delta = getRotationDelta(prev, c.targetRotation);
            if (delta < bestDelta) {
                bestDelta = delta;
                selected = c;
            }
        }

        return selected != null ? selected : valid.get(0);
    }

    private void doPlaceCrystal(PlaceCandidate candidate, FindItemResult crystalItem) {
        boolean swapped = false;
        InteractionHand hand = crystalItem.getHand();
        if (crystalItem.slot() != mc.player.getInventory().getSelectedSlot() && crystalItem.slot() != 40) {
            switch (placeSwapMode.getValue()) {
                case Swap -> InvUtils.swap(crystalItem.slot(), false);
                case Silent -> swapped = InvUtils.swap(crystalItem.slot(), true);
                default -> {
                }
            }
            hand = InteractionHand.MAIN_HAND;
        }
        final boolean silentSwapped = swapped;
        final InteractionHand placeHand = hand;
        final int requestPriority = Priority.High.priority;
        final Vector2f rotation = candidate.targetRotation;
        final Vec3 hitVec = new Vec3(
                candidate.supportPos.getX() + 0.5,
                candidate.supportPos.getY() + 1.0,
                candidate.supportPos.getZ() + 0.5
        );
        RotationManager.INSTANCE.applyRotation(rotation, placeRotationSpeed.getValue(), requestPriority, record -> {
            if (!isEnabled() || nullCheck()) {
                if (silentSwapped) InvUtils.swapBack();
                return;
            }
            if (record.selectedPriorityValue() != requestPriority) return;
            boolean canPlace = candidate.throughWall
                    ? candidate.wallBypassAllowed && isAimingAtBlock(record.currentRotation(), candidate.targetRotation)
                    : RaytraceUtils.overBlock(record.currentRotation(), Direction.UP, candidate.supportPos, false);
            if (!canPlace) {
                if (silentSwapped) InvUtils.swapBack();
                return;
            }

            BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, candidate.supportPos, false);
            InteractionResult result = mc.gameMode.useItemOn(mc.player, placeHand, hitResult);
            if (result.consumesAction()) {
                doSwing(placeSwing.getValue());
                addRenderRecord(candidate.supportPos, candidate.targetDmg);
                placeTimer.reset();
            }
            if (silentSwapped) InvUtils.swapBack();
        });
    }

    /**
     * Combined self-damage check — returns {@code true} when the crystal should be
     * <b>rejected</b> (self-damage is too high).
     * <ol>
     *   <li>selfDmg must not exceed the per-action max (placeMaxSelfDmg / breakMaxSelfDmg)</li>
     *   <li>noSuicide: selfDmg must not kill the player (hp + absorption - selfDmg &gt; 0.5)</li>
     *   <li>lethalMaxSelfDamage: when the remaining HP would be critically low, selfDmg
     *       must also be below lethalMaxSelfDamage.</li>
     * </ol>
     */
    private boolean exceedsSelfDamageLimit(float selfDmg, double maxSelfDmg) {
        if (selfDmg > maxSelfDmg) return true;

        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (noSuicide.getValue() && hp - selfDmg <= 0.5f) return true;
        float remainingHp = hp - selfDmg;
        if (noSuicide.getValue() && remainingHp <= lethalMaxSelfDamage.getValue()
                && selfDmg > lethalMaxSelfDamage.getValue()) {
            return true;
        }

        return false;
    }

    private Vec3 getPredictedTargetPos(LivingEntity entity) {
        if (!motionPrediction.getValue()) {
            return entity.position();
        }

        double motionX = Mth.clamp(entity.getX() - entity.xo, -0.6, 0.6);
        double motionY = Mth.clamp(entity.getY() - entity.yo, -0.5, 0.5);
        double motionZ = Mth.clamp(entity.getZ() - entity.zo, -0.6, 0.6);

        AABB entityBox = entity.getBoundingBox();
        AABB targetBox = entityBox;
        int ticks = predictTick.getValue();

        for (int tick = 0; tick <= ticks; tick++) {
            AABB moved = canMove(entity, targetBox, motionX, motionY, motionZ);
            if (moved == null) moved = canMove(entity, targetBox, motionX, 0.0, motionZ);
            if (moved == null) moved = canMove(entity, targetBox, 0.0, motionY, 0.0);
            if (moved == null) break;
            targetBox = moved;
        }

        double offsetX = targetBox.minX - entityBox.minX;
        double offsetY = targetBox.minY - entityBox.minY;
        double offsetZ = targetBox.minZ - entityBox.minZ;
        return entity.position().add(offsetX, offsetY, offsetZ);
    }

    private AABB canMove(Entity entity, AABB box, double motionX, double motionY, double motionZ) {
        AABB moved = box.move(motionX, motionY, motionZ);
        return mc.level.noCollision(entity, moved) ? moved : null;
    }

    private boolean isAimingAtBlock(Vector2f currentRotation, Vector2f targetRotation) {
        return getRotationDelta(currentRotation, targetRotation) <= 2.5f;
    }

    private FindItemResult findCrystalItem() {
        return InvUtils.findInHotbar(Items.END_CRYSTAL);
    }

    private void doSwing(SwingMode mode) {
        switch (mode) {
            case Client -> mc.player.swing(InteractionHand.MAIN_HAND);
            case Packet -> mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            default -> {
            }
        }
    }

    private void addRenderRecord(BlockPos pos, float targetDamage) {
        renderRecords.removeIf(r -> r.pos.equals(pos));
        renderRecords.add(new RenderRecord(pos, targetDamage, renderTimer.getMs()));
    }

    @SubscribeEvent
    private void onRender3D(RenderLevelStageEvent.AfterLevel event) {
        if (nullCheck()) return;
        if (renderRecords.isEmpty()) return;

        long now = renderTimer.getMs();
        long totalLife = movingLength.getValue() + fadeLength.getValue();
        renderRecords.removeIf(r -> now - r.time > totalLife);

        List<TextDrawData> textDraws = new ArrayList<>();

        for (RenderRecord record : renderRecords) {
            long age = now - record.time;
            float alpha;

            if (age <= movingLength.getValue()) {
                alpha = 1.0f;
            } else {
                float fadeProgress = (float) (age - movingLength.getValue()) / Math.max(1, fadeLength.getValue());
                alpha = 1.0f - Math.min(1.0f, fadeProgress);
            }

            if (alpha <= 0.01f) continue;

            Color fc = filledColor.getValue();
            Color oc = outlineColor.getValue();

            Color filled = new Color(fc.getRed(), fc.getGreen(), fc.getBlue(),
                    Math.max(0, Math.min(255, (int) (fc.getAlpha() * alpha))));
            Color outline = new Color(oc.getRed(), oc.getGreen(), oc.getBlue(),
                    Math.max(0, Math.min(255, (int) (oc.getAlpha() * alpha))));

            AABB box = new AABB(record.pos);
            Render3DUtils.drawFilledBox(box, filled);
            Render3DUtils.drawOutlineBox(event.getPoseStack(), box, outline.getRGB(), outlineWidth.getValue().floatValue());

            if (!showDamage.getValue()) continue;

            Vector2f screenPos = projectRecordToScreen(record.pos.below());
            if (screenPos == null) continue;

            TextRenderer textRenderer = rectRenderer.get();
            float textScale = 1.0f;
            String text = String.format(Locale.ROOT, "%.1f", record.targetDamage);
            float textWidth = textRenderer.getWidth(text, textScale);
            float textHeight = textRenderer.getHeight(textScale);
            Color textColor = new Color(255, 255, 255, Math.max(0, Math.min(255, (int) (220 * alpha))));
            textDraws.add(new TextDrawData(text, screenPos.x - textWidth / 2.0f, screenPos.y - textHeight, textScale, textColor));
        }

        if (!textDraws.isEmpty()) {
            RenderManager.INSTANCE.applyRenderWorldHud(() -> {
                TextRenderer textRenderer = rectRenderer.get();
                for (TextDrawData text : textDraws) {
                    textRenderer.addText(text.text, text.x, text.y, text.scale, text.color);
                }
                textRenderer.drawAndClear();
            });
        }
    }

    private Vector2f projectRecordToScreen(BlockPos pos) {
        int[] viewport = new int[]{0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight()};
        CameraRenderState cameraState = mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        Matrix4f viewProjectionMatrix = new Matrix4f(cameraState.projectionMatrix).mul(cameraState.viewRotationMatrix);

        Vector4d projected = WorldToScreen.projectEntity(viewport, viewProjectionMatrix, new AABB(pos));
        if (projected == null) return null;

        double guiScale = mc.getWindow().getGuiScale();
        double minX = projected.x / guiScale;
        double minY = projected.y / guiScale;
        double maxX = projected.z / guiScale;
        double maxY = projected.w / guiScale;

        float screenWidth = mc.getWindow().getGuiScaledWidth();
        float screenHeight = mc.getWindow().getGuiScaledHeight();

        if (maxX < 0 || maxY < 0 || minX > screenWidth || minY > screenHeight) return null;
        return new Vector2f((float) ((minX + maxX) * 0.5), (float) minY);
    }

    private record PlaceCandidate(
            BlockPos supportPos,
            Vec3 crystalPos,
            float targetDmg,
            float selfDmg,
            Vector2f targetRotation,
            boolean throughWall,
            boolean wallBypassAllowed
    ) {
    }

    private EndCrystal selectBestBreakCandidate(List<BreakCandidate> candidates) {
        if (candidates.isEmpty()) return null;

        float bestDamage = Float.MIN_VALUE;
        BreakCandidate maxDamage = null;
        for (BreakCandidate c : candidates) {
            if (c.targetDmg > bestDamage) {
                bestDamage = c.targetDmg;
                maxDamage = c;
            }
        }

        if (maxDamage == null || aimOptimizeMode.is(AimOptimizeMode.MaxDamage)) {
            return maxDamage != null ? maxDamage.crystal : null;
        }

        float threshold = bestDamage - aimDamageSacrifice.getValue().floatValue();
        BreakCandidate selected = null;
        float bestDelta = Float.MAX_VALUE;
        for (BreakCandidate c : candidates) {
            if (c.targetDmg < threshold) continue;
            if (c.rotationDelta < bestDelta) {
                bestDelta = c.rotationDelta;
                selected = c;
            }
        }

        return selected != null ? selected.crystal : maxDamage.crystal;
    }

    private Vector2f getPrevTickRotation() {
        Vector2f prev = RotationManager.INSTANCE.lastRotations;
        if (prev == null) {
            return new Vector2f(mc.player.getYRot(), mc.player.getXRot());
        }
        return new Vector2f(prev.x, prev.y);
    }

    private float getRotationDelta(Vector2f from, Vector2f to) {
        float yawDiff = Math.abs(Mth.wrapDegrees(to.x - from.x));
        float pitchDiff = Math.abs(to.y - from.y);
        return (float) Math.hypot(yawDiff, pitchDiff);
    }

    private record BreakCandidate(
            EndCrystal crystal,
            float targetDmg,
            Vector2f targetRotation,
            float rotationDelta
    ) {
    }

    private record RenderRecord(BlockPos pos, float targetDamage, long time) {
    }

    private record TextDrawData(String text, float x, float y, float scale, Color color) {
    }

    private enum SwapMode {
        None,
        Swap,
        Silent,
    }

    private enum SwingMode {
        None,
        Client,
        Packet,
    }

    private enum AimOptimizeMode {
        MaxDamage,
        LowRotation
    }

}
