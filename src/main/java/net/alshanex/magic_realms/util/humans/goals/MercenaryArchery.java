package net.alshanex.magic_realms.util.humans.goals;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.MercenaryBowFakePlayer;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class MercenaryArchery {

    private final AbstractMercenaryEntity entity;

    private MercenaryBowFakePlayer bowFakePlayer;
    private boolean wasChargingArrow = false;
    private boolean shouldPlayChargeAnimation = false;

    public MercenaryArchery(AbstractMercenaryEntity entity) {
        this.entity = entity;
    }

    // ====== State accessors (read by animation predicate) ======

    public boolean shouldPlayChargeAnimation() {
        return shouldPlayChargeAnimation;
    }

    // ====== Bow fake player ======

    public MercenaryBowFakePlayer getOrCreateBowFakePlayer(ServerLevel level) {
        if (bowFakePlayer == null || bowFakePlayer.level() != level) {
            bowFakePlayer = new MercenaryBowFakePlayer(level);
        }
        return bowFakePlayer;
    }

    // ====== Charge animation tick ======

    /** Called each tick for archers to keep charge-animation flags in sync. */
    public void tickAnimations() {
        boolean isCurrentlyCharging = isChargingArrow();

        if (isCurrentlyCharging && !wasChargingArrow) {
            shouldPlayChargeAnimation = true;
            wasChargingArrow = true;
        } else if (!isCurrentlyCharging && wasChargingArrow) {
            shouldPlayChargeAnimation = false;
            wasChargingArrow = false;
        }
    }

    public boolean isChargingArrow() {
        if (!entity.isUsingItem()) return false;
        ItemStack mainHand = entity.getMainHandItem();
        return mainHand.getItem() instanceof BowItem || mainHand.is(ModTags.BOWS);
    }

    // ====== Arrow inventory lookup ======

    public boolean canPerformRangedAttack() {
        return entity.isArcher() && hasArrows();
    }

    public boolean hasArrows() {
        SimpleContainer inventory = entity.getInventory();
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isArrowItem(stack)) {
                count += stack.getCount();
            }
        }
        return count > 0;
    }

    private static boolean isArrowItem(ItemStack stack) {
        return stack.getItem() instanceof ArrowItem || stack.is(Items.ARROW);
    }

    private ItemStack getBestArrowFromInventory() {
        SimpleContainer inventory = entity.getInventory();

        // Prefer special arrow types over vanilla arrows
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isArrowItem(stack) && !stack.is(Items.ARROW) && !stack.isEmpty()) {
                return stack;
            }
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.ARROW) && !stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private void consumeArrow() {
        SimpleContainer inventory = entity.getInventory();

        // Consume special arrows first, then fall back to vanilla
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isArrowItem(stack) && !stack.is(Items.ARROW) && !stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) inventory.setItem(i, ItemStack.EMPTY);
                return;
            }
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.ARROW) && !stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) inventory.setItem(i, ItemStack.EMPTY);
                return;
            }
        }
    }

    // ====== Ranged attack ======

    /**
     * Fires an arrow at the target using the modded bow's release pipeline. Falls back to vanilla projectile physics if the modded bow throws.
     */
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        if (!hasArrows()) return;

        ItemStack bow = entity.getMainHandItem();
        if (!(bow.getItem() instanceof BowItem || bow.is(ModTags.BOWS))) return;

        ItemStack arrowStack = getBestArrowFromInventory();
        if (arrowStack.isEmpty()) arrowStack = new ItemStack(Items.ARROW);

        MercenaryBowFakePlayer fp = getOrCreateBowFakePlayer(serverLevel);

        // Aim at target, compensating for arrow drop
        Vec3 shooterPos = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
        Vec3 targetPos = new Vec3(target.getX(), target.getY(0.5), target.getZ());
        double dx = targetPos.x - shooterPos.x;
        double dy = targetPos.y - shooterPos.y;
        double dz = targetPos.z - shooterPos.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        // Arrow physics
        float arrowVelocity = 2.5F;
        double timeTicks = horiz / arrowVelocity;
        double drop = 0.05 * timeTicks * timeTicks * 0.5;

        double aimY = dy + drop;
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-Mth.atan2(aimY, horiz) * (180.0 / Math.PI));

        // Use a COPY of the bow so durability damage doesn't affect our real item mid-flight.
        ItemStack bowCopy = bow.copy();
        fp.syncFrom(entity, bowCopy, arrowStack.copyWithCount(1));
        fp.setXRot(pitch);
        fp.setYRot(yaw);
        fp.xRotO = pitch;
        fp.yRotO = yaw;
        fp.yHeadRot = yaw;
        fp.yHeadRotO = yaw;
        fp.yBodyRot = yaw;
        fp.yBodyRotO = yaw;

        boolean fired = false;
        try {
            int useDuration = bow.getUseDuration(fp);
            int chargeTime = useDuration - 20;  // 20 ticks = full draw
            fp.startUsingItem(InteractionHand.MAIN_HAND);
            bow.releaseUsing(serverLevel, fp, chargeTime);
            fired = true;
        } catch (Throwable t) {
            MagicRealms.LOGGER.error("Modded bow {} threw during releaseUsing — falling back to vanilla shot",
                    BuiltInRegistries.ITEM.getKey(bow.getItem()), t);
        } finally {
            try { fp.stopUsingItem(); } catch (Throwable ignored) {}
        }

        // Copy durability damage from bowCopy back to the real bow
        int damageTaken = bowCopy.getDamageValue() - bow.getDamageValue();
        if (damageTaken > 0) {
            bow.setDamageValue(bow.getDamageValue() + damageTaken);
            if (bow.getDamageValue() >= bow.getMaxDamage()) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                entity.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + entity.getRandom().nextFloat() * 0.4F);
            }
        }

        if (!fired) {
            fallbackVanillaShoot(target);
            return;
        }

        consumeArrow();
        entity.playSound(SoundEvents.SKELETON_SHOOT, 0.8F, 1.2F);
    }

    /** Vanilla fallback if the modded bow's release pipeline fails. */
    private void fallbackVanillaShoot(LivingEntity target) {
        AbstractArrow abstractarrow = createArrow();
        if (abstractarrow == null) return;

        double d0 = target.getX() - entity.getX();
        double d1 = target.getY(0.3333) - abstractarrow.getY();
        double d2 = target.getZ() - entity.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        abstractarrow.shoot(d0, d1 + d3 * 0.2, d2, 1.6f, 8f);
        entity.playSound(SoundEvents.SKELETON_SHOOT, 0.8F, 1.2F);
        entity.level().addFreshEntity(abstractarrow);
        consumeArrow();
    }

    private AbstractArrow createArrow() {
        if (!hasArrows()) return null;

        ItemStack arrowStack = getBestArrowFromInventory();
        if (arrowStack.isEmpty()) {
            return new Arrow(entity.level(), entity, arrowStack.copyWithCount(1), entity.getMainHandItem());
        }

        if (arrowStack.getItem() instanceof ArrowItem arrowItem) {
            return arrowItem.createArrow(entity.level(), arrowStack, entity, entity.getMainHandItem());
        }

        return new Arrow(entity.level(), entity, arrowStack.copyWithCount(1), entity.getMainHandItem());
    }
}
