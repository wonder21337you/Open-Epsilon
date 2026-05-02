package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.impl.SendPositionEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.utils.player.MoveUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.phys.AABB;

public class FastWeb extends Module {

    public static final FastWeb INSTANCE = new FastWeb();

    private FastWeb() {
        super("Fast Web", Category.PLAYER);
    }

    private enum Mode {
        Vanilla,
        Grim
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Grim);

    private final BoolSetting onlyOnGround = boolSetting("Only On Ground", false, () -> mode.is(Mode.Grim));
    private final BoolSetting motionY = boolSetting("Motion Y", false, () -> mode.is(Mode.Grim));

    @EventHandler
    private void onSendPosition(SendPositionEvent event) {
        if (nullCheck() || mode.is(Mode.Vanilla)) return;

        if (!isInWeb()) {
            return;
        }

        if (!MoveUtils.isMoving()) {
            return;
        }

        if (mc.player.onGround() || !onlyOnGround.getValue()) {
            double[] forward = MoveUtils.forward(0.63);
            mc.player.setDeltaMovement(forward[0], mc.player.getDeltaMovement().y, forward[1]);
        }

        if (motionY.getValue()) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.1, mc.player.getDeltaMovement().z);
        }
    }

    private boolean isInWeb() {
        AABB box = mc.player.getBoundingBox().deflate(1.0E-6);

        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutablePos.set(x, y, z);
                    if (mc.level.getBlockState(mutablePos).getBlock() instanceof WebBlock) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean cobweb() {
        return isEnabled() && mode.is(Mode.Vanilla);
    }

}
