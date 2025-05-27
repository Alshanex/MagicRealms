package net.alshanex.magic_realms.util;

import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.BowItem;

import java.util.EnumSet;

public class ChargeArrowAttackGoal<T extends RandomHumanEntity & RangedAttackMob> extends Goal {
    private final T mob;
    private final double speedModifier;
    private int attackIntervalMin;
    private final float attackRadiusSqr;
    private int attackTime = -1;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;
    private boolean isCharging = false;
    private int chargingTime = 0;

    public ChargeArrowAttackGoal(T pMob, double pSpeedModifier, int pAttackInterval, float pAttackRadius) {
        this.mob = pMob;
        this.speedModifier = pSpeedModifier;
        this.attackIntervalMin = pAttackInterval;
        this.attackRadiusSqr = pAttackRadius * pAttackRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        return this.mob.getTarget() != null && this.isHoldingBow();
    }

    protected boolean isHoldingBow() {
        return this.mob.isHolding(is -> is.getItem() instanceof BowItem);
    }

    public boolean canContinueToUse() {
        return (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingBow();
    }

    public void start() {
        super.start();
        this.mob.setAggressive(true);
        this.isCharging = false;
        this.chargingTime = 0;
    }

    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.seeTime = 0;
        this.attackTime = -1;
        this.isCharging = false;
        this.chargingTime = 0;
        this.mob.stopUsingItem();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            double distanceSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            boolean canSee = this.mob.getSensing().hasLineOfSight(target);
            boolean wasSeen = this.seeTime > 0;

            if (canSee != wasSeen) {
                this.seeTime = 0;
            }

            if (canSee) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            if (!(distanceSqr > (double)this.attackRadiusSqr) && this.seeTime >= 20) {
                this.mob.getNavigation().stop();
                ++this.strafingTime;
            } else {
                this.mob.getNavigation().moveTo(target, this.speedModifier);
                this.strafingTime = -1;
            }

            if (this.strafingTime >= 20) {
                if ((double)this.mob.getRandom().nextFloat() < 0.3D) {
                    this.strafingClockwise = !this.strafingClockwise;
                }

                if ((double)this.mob.getRandom().nextFloat() < 0.3D) {
                    this.strafingBackwards = !this.strafingBackwards;
                }

                this.strafingTime = 0;
            }

            if (this.strafingTime > -1) {
                if (distanceSqr > (double)(this.attackRadiusSqr * 0.75F)) {
                    this.strafingBackwards = false;
                } else if (distanceSqr < (double)(this.attackRadiusSqr * 0.25F)) {
                    this.strafingBackwards = true;
                }

                this.mob.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
                this.mob.lookAt(target, 30.0F, 30.0F);
            } else {
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            if (this.mob.isUsingItem()) {
                if (!canSee && this.seeTime < -60) {
                    this.mob.stopUsingItem();
                    this.isCharging = false;
                    this.chargingTime = 0;
                } else if (canSee) {
                    int useTime = this.mob.getTicksUsingItem();

                    if (useTime == 1) {
                        this.isCharging = true;
                        this.chargingTime = 0;
                        this.mob.playSound(SoundEvents.CROSSBOW_LOADING_START.value(), 0.7F, 1.0F);
                    }

                    if (this.isCharging) {
                        this.chargingTime++;
                    }

                    if (useTime >= 20) {
                        this.mob.stopUsingItem();
                        this.isCharging = false;
                        this.chargingTime = 0;

                        this.mob.performRangedAttack(target, BowItem.getPowerForTime(useTime));
                        this.attackTime = this.attackIntervalMin;
                    }
                }
            } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
                this.mob.startUsingItem(InteractionHand.MAIN_HAND);
            }
        }
    }

    public boolean isCharging() {
        return this.isCharging;
    }

    public int getChargingTime() {
        return this.chargingTime;
    }
}
