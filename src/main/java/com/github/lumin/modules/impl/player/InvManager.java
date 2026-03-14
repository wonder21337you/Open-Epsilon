package com.github.lumin.modules.impl.player;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.settings.impl.IntSetting;

public class InvManager extends Module {

    public static final InvManager INSTANCE = new InvManager();

    private InvManager() {
        super("InvManager", Category.PLAYER);
    }

    public enum OffhandItemMode {
        None,
        GoldenApple,
        Projectile,
        FishingRod,
        Block
    }

    public enum BowPriority {
        Crossbow,
        PowerBow,
        PunchBow
    }

    private final DoubleSetting minDelay = doubleSetting("Min Delay", 90.0, 0.0, 500.0, 5.0);
    private final DoubleSetting maxDelay = doubleSetting("Max Delay", 110.0, 0.0, 500.0, 5.0);
    private final EnumSetting<OffhandItemMode> offhandItems = enumSetting("Offhand Items", OffhandItemMode.Projectile);
    private final BoolSetting autoArmor = boolSetting("Auto Armor", true);
    private final BoolSetting inventoryOnly = boolSetting("Inventory Only", true);
    private final BoolSetting switchSword = boolSetting("Switch Sword", true);
    private final IntSetting swordSlot = intSetting("Sword Slot", 1, 1, 9, 1, switchSword::getValue);
    private final BoolSetting switchBlock = boolSetting("Switch Block", true, () -> !offhandItems.is(OffhandItemMode.Block));
    private final IntSetting blockSlot = intSetting("Block Slot", 9, 1, 9, 1, () -> switchBlock.getValue() && !offhandItems.is(OffhandItemMode.Block));
    public final IntSetting maxBlockSize = intSetting("Max Block Size", 256, 64, 512, 64, switchBlock::getValue);
    private final BoolSetting switchPickaxe = boolSetting("Switch Pickaxe", true);
    private final IntSetting pickaxeSlot = intSetting("Pickaxe Slot", 3, 1, 9, 1, switchPickaxe::getValue);
    private final BoolSetting switchAxe = boolSetting("Switch Axe", true);
    private final IntSetting axeSlot = intSetting("Axe Slot", 4, 1, 9, 1, switchAxe::getValue);
    private final BoolSetting switchBow = boolSetting("Switch Bow or Crossbow", true);
    private final IntSetting bowSlot = intSetting("Bow Slot", 5, 1, 9, 1, switchBow::getValue);
    private final EnumSetting<BowPriority> preferBow = enumSetting("Bow Priority", BowPriority.Crossbow, switchBow::getValue);
    public final IntSetting maxArrowSize = intSetting("Max Arrow Size", 256, 64, 512, 64, switchBow::getValue);
    private final BoolSetting switchWaterBucket = boolSetting("Switch Water Bucket", true);
    private final IntSetting waterBucketSlot = intSetting("Water Bucket Slot", 6, 1, 9, 1, switchWaterBucket::getValue);
    private final BoolSetting switchEnderPearl = boolSetting("Switch Ender Pearl", true);
    private final IntSetting enderPearlSlot = intSetting("Ender Pearl Slot", 7, 1, 9, 1, switchEnderPearl::getValue);
    private final BoolSetting switchFireball = boolSetting("Switch Fireball", true);
    private final IntSetting fireballSlot = intSetting("Fireball Slot", 8, 1, 9, 1, switchFireball::getValue);
    private final BoolSetting switchGoldenApple = boolSetting("Switch Golden Apple", true, () -> !offhandItems.is(OffhandItemMode.GoldenApple));
    private final IntSetting goldenAppleSlot = intSetting("Golden Apple Slot", 2, 1, 9, 1, () -> switchGoldenApple.getValue() && !offhandItems.is(OffhandItemMode.GoldenApple));
    private final BoolSetting throwItems = boolSetting("Throw Items", true);
    public final IntSetting waterBucketCount = intSetting("Keep Water Buckets", 1, 0, 5, 1, throwItems::getValue);
    public final IntSetting lavaBucketCount = intSetting("Keep Lava Buckets", 1, 0, 5, 1, throwItems::getValue);
    public final BoolSetting keepProjectile = boolSetting("Keep Eggs & Snowballs", true);
    private final BoolSetting switchProjectile = boolSetting("Switch Eggs & Snowballs", true, () -> keepProjectile.getValue() && !offhandItems.is(OffhandItemMode.Projectile));
    private final IntSetting projectileSlot = intSetting("Eggs & Snowballs Slot", 9, 1, 9, 1, () -> switchProjectile.getValue() && keepProjectile.getValue() && !offhandItems.is(OffhandItemMode.Projectile));
    public final IntSetting maxProjectileSize = intSetting("Max Eggs & Snowballs Size", 64, 16, 256, 16, keepProjectile::getValue);
    private final BoolSetting switchRod = boolSetting("Switch Rod", false, () -> !offhandItems.is(OffhandItemMode.FishingRod));
    private final IntSetting rodSlot = intSetting("Rod Slot", 9, 1, 9, 1, () -> switchRod.getValue() && !offhandItems.is(OffhandItemMode.FishingRod));


}
