package net.alshanex.magic_realms.entity.slime;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.registry.MRSpellRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SummonedMagicSlimeEntity extends MagicSlimeEntity implements IMagicSummon {
    public SummonedMagicSlimeEntity(EntityType<? extends MagicSlimeEntity> pEntityType, Level pLevel) {
        super(MREntityRegistry.SUMMONED_MAGIC_SLIME.get(), pLevel);
        xpReward = 0;
    }

    @Deprecated(forRemoval = true)
    public SummonedMagicSlimeEntity(Level pLevel, LivingEntity owner) {
        this(MREntityRegistry.SUMMONED_MAGIC_SLIME.get(), pLevel);
        setSummoner(owner);
    }

    public SummonedMagicSlimeEntity(Level pLevel, LivingEntity owner, SchoolType parentSchool) {
        this(MREntityRegistry.SUMMONED_MAGIC_SLIME.get(), pLevel);
        setSummoner(owner);
        if (parentSchool != null) {
            this.setWeakSchool(parentSchool);
        } else {
            this.initializeSchool();
        }
    }

    @Override
    public void registerGoals() {
        super.registerGoals();

        this.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal);

        this.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(3, new SlimeCopyOwnerTargetGoal(this, this::getSummoner));
        this.targetSelector.addGoal(4, (new SlimeHurtByTargetGoal(this, (entity) -> entity == getSummoner())).setAlertOthers());
        this.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(this, this::getSummoner));
    }

    @Override
    protected void spawnChildren() {
        int i = this.getSize();
        if (!this.level().isClientSide && i > 1 && this.isDeadOrDying()) {
            Component component = this.getCustomName();
            boolean flag = this.isNoAi();
            float f = this.getDimensions(this.getPose()).width();
            float f1 = f / 2.0F;
            int j = i / 2;
            int k = 2 + this.random.nextInt(3);

            var children = new java.util.ArrayList<Mob>();

            for (int l = 0; l < k; l++) {
                float f2 = ((float)(l % 2) - 0.5F) * f1;
                float f3 = ((float)(l / 2) - 0.5F) * f1;

                SchoolType parentSchool = this.getWeakSchool();
                SummonedMagicSlimeEntity slime = new SummonedMagicSlimeEntity(this.level(), (LivingEntity) getSummoner(), parentSchool);

                if (slime != null) {
                    if (this.isPersistenceRequired()) {
                        slime.setPersistenceRequired();
                    }

                    slime.setCustomName(component);
                    slime.setNoAi(flag);
                    slime.setInvulnerable(this.isInvulnerable());
                    slime.setSize(j, true);
                    slime.hasDivided = false;
                    slime.markHurt();

                    slime.moveTo(this.getX() + (double)f2, this.getY() + 0.5, this.getZ() + (double)f3, this.random.nextFloat() * 360.0F, 0.0F);

                    children.add(slime);
                }
            }

            if (!net.neoforged.neoforge.event.EventHooks.onMobSplit(this, children).isCanceled()) {
                children.forEach(this.level()::addFreshEntity);
            }

            // Temporarily set size to 1 to prevent vanilla splitting
            this.setSize(1, false);
        }
    }

    @Override
    protected DamageSource getDamageSource() {
        return MRSpellRegistry.SLIME_RAIN.get().getDamageSource(this, getSummoner());
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (shouldIgnoreDamage(pSource))
            return false;
        return super.hurt(pSource, pAmount);
    }

    @Deprecated(forRemoval = true)
    public void setSummoner(@Nullable LivingEntity owner) {
        if (owner == null) return;
        SummonManager.setOwner(this, owner);
    }

    @Override
    public void die(DamageSource pDamageSource) {
        this.onDeathHelper();
        super.die(pDamageSource);
    }

    @Override
    public void onRemovedFromLevel() {
        this.onRemovedHelper(this);
        super.onRemovedFromLevel();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {

    }

    @Override
    protected void dropAllDeathLoot(ServerLevel p_level, DamageSource damageSource) {

    }

    @Override
    protected void dropFromLootTable(DamageSource damageSource, boolean attackedRecently) {

    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        return super.isAlliedTo(pEntity) || this.isAlliedHelper(pEntity);
    }

    public void onUnSummon() {
        if (!level().isClientSide) {
            MagicManager.spawnParticles(level(), ParticleTypes.POOF, getX(), getY(), getZ(), 25, .4, .8, .4, .03, false);
            setRemoved(RemovalReason.DISCARDED);
        }
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    public class SlimeCopyOwnerTargetGoal extends TargetGoal {
        private final Supplier<Entity> ownerGetter;

        public SlimeCopyOwnerTargetGoal(Mob pMob, Supplier<Entity> ownerGetter) {
            super(pMob, false);
            this.ownerGetter = ownerGetter;

        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        public boolean canUse() {
            if (!(ownerGetter.get() instanceof Mob owner)) {
                return false;
            }
            var target = owner.getTarget();
            if (target == null) {
                return false;
            }
            return canAttack(target, TargetingConditions.DEFAULT) && !(target instanceof IMagicSummon summon && summon.getSummoner() == owner);
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        public void start() {
            var target = ((Mob) ownerGetter.get()).getTarget();
            mob.setTarget(target);
            this.mob.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, target, 200L);

            super.start();
        }
    }

    public class SlimeHurtByTargetGoal extends TargetGoal {
        private static final TargetingConditions HURT_BY_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();
        private static final int ALERT_RANGE_Y = 10;
        private boolean alertSameType;
        /**
         * Store the previous revengeTimer value
         */
        private int timestamp;
        Predicate<LivingEntity> toIgnoreDamage;
        @Nullable
        private Class<?>[] toIgnoreAlert;

        public SlimeHurtByTargetGoal(Mob pMob, Predicate<LivingEntity> pToIgnoreDamage) {
            super(pMob, true);
            this.toIgnoreDamage = pToIgnoreDamage;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        public boolean canUse() {
            int i = this.mob.getLastHurtByMobTimestamp();
            LivingEntity livingentity = this.mob.getLastHurtByMob();
            if (livingentity == null || livingentity.isAlliedTo(mob))
                return false;
            if (i != this.timestamp && livingentity != null) {
                if (livingentity.getType() == EntityType.PLAYER && this.mob.level().getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                    return false;
                } else {
                    if (toIgnoreDamage.test(livingentity))
                        return false;

                    return this.canAttack(livingentity, HURT_BY_TARGETING);
                }
            } else {
                return false;
            }
        }

        public SlimeHurtByTargetGoal setAlertOthers(Class<?>... pReinforcementTypes) {
            this.alertSameType = true;
            this.toIgnoreAlert = pReinforcementTypes;
            return this;
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        public void start() {
            this.mob.setTarget(this.mob.getLastHurtByMob());
            this.mob.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, this.mob.getLastHurtByMob(), 200L);

            this.targetMob = this.mob.getTarget();
            this.timestamp = this.mob.getLastHurtByMobTimestamp();
            this.unseenMemoryTicks = 300;
            if (this.alertSameType) {
                this.alertOthers();
            }

            super.start();
        }

        protected void alertOthers() {
            double d0 = this.getFollowDistance();
            AABB aabb = AABB.unitCubeFromLowerCorner(this.mob.position()).inflate(d0, 10.0D, d0);
            List<? extends Mob> list = this.mob.level().getEntitiesOfClass(this.mob.getClass(), aabb, EntitySelector.NO_SPECTATORS);
            Iterator iterator = list.iterator();

            while (true) {
                Mob mob;
                while (true) {
                    if (!iterator.hasNext()) {
                        return;
                    }

                    mob = (Mob) iterator.next();
                    if (this.mob != mob && mob.getTarget() == null && (!(this.mob instanceof TamableAnimal) || ((TamableAnimal) this.mob).getOwner() == ((TamableAnimal) mob).getOwner()) && !mob.isAlliedTo(this.mob.getLastHurtByMob())) {
                        if (this.toIgnoreAlert == null) {
                            break;
                        }

                        boolean flag = false;

                        for (Class<?> oclass : this.toIgnoreAlert) {
                            if (mob.getClass() == oclass) {
                                flag = true;
                                break;
                            }
                        }

                        if (!flag) {
                            break;
                        }
                    }
                }

                this.alertOther(mob, this.mob.getLastHurtByMob());
            }
        }

        protected void alertOther(Mob pMob, LivingEntity pTarget) {
            pMob.setTarget(pTarget);
        }
    }
}
