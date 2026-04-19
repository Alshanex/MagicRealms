package net.alshanex.magic_realms.util.humans.goals;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericCopyOwnerTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtByTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericProtectOwnerTargetGoal;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.alshanex.magic_realms.block.ChairBlock;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.exclusive.amadeus.AmadeusEntity;
import net.alshanex.magic_realms.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

public class HumanGoals {

    public static class CustomFearGoal extends AvoidEntityGoal<LivingEntity> {
        private final AbstractMercenaryEntity humanEntity;

        public CustomFearGoal(AbstractMercenaryEntity humanEntity, float maxDistance, double walkSpeedModifier, double sprintSpeedModifier) {
            super(humanEntity, LivingEntity.class, maxDistance, walkSpeedModifier, sprintSpeedModifier,
                    (livingEntity) -> humanEntity.isAfraidOf(livingEntity));
            this.humanEntity = humanEntity;
        }

        @Override
        public boolean canUse() {
            if(this.humanEntity.isStunned()){
                return false;
            }
            if (this.humanEntity.isSittingInChair()) {
                return false;
            }
            boolean canUse = super.canUse();
            if (canUse) {
                humanEntity.setTarget(null);
            }
            return canUse;
        }

        @Override
        public void start() {
            if(this.humanEntity instanceof AmadeusEntity amadeusEntity && !amadeusEntity.level().isClientSide && amadeusEntity.getSummoner() != null){
                if(hasContractorNearby(amadeusEntity, amadeusEntity.getSummoner(), amadeusEntity.level())){
                    amadeusEntity.getSummoner().sendSystemMessage(Component.translatable("message.magic_realms.amadeus.enderman.scared", amadeusEntity.getDisplayName()).withStyle(ChatFormatting.GOLD));
                }
            }
            super.start();
        }

        private boolean hasContractorNearby(AmadeusEntity amadeus, LivingEntity entity, Level level) {
            double SEARCH_RADIUS = 16.0;
            AABB searchArea = new AABB(
                    amadeus.getX() - SEARCH_RADIUS,
                    amadeus.getY() - SEARCH_RADIUS,
                    amadeus.getZ() - SEARCH_RADIUS,
                    amadeus.getX() + SEARCH_RADIUS,
                    amadeus.getY() + SEARCH_RADIUS,
                    amadeus.getZ() + SEARCH_RADIUS
            );

            List<Player> nearbyContractor = level.getEntitiesOfClass(
                    Player.class,
                    searchArea,
                    player1 -> player1.is(entity)
            );

            return !nearbyContractor.isEmpty();
        }

        @Override
        public void tick() {
            super.tick();
            if (humanEntity.getTarget() != null && humanEntity.isAfraidOf(humanEntity.getTarget())) {
                humanEntity.setTarget(null);
            }
        }
    }

    public static class NoFearTargetGoal extends NearestAttackableTargetGoal<Monster> {
        private final AbstractMercenaryEntity humanEntity;

        public NoFearTargetGoal(AbstractMercenaryEntity humanEntity) {
            super(humanEntity, Monster.class, 10, true, false,
                    (entity) -> !(entity instanceof Creeper));
            this.humanEntity = humanEntity;
        }

        @Override
        public boolean canUse() {
            if(this.humanEntity.isStunned()){
                return false;
            }
            if (this.humanEntity.isSittingInChair()) {
                return false;
            }
            boolean canUse = super.canUse();
            if (canUse && target != null && humanEntity.isAfraidOf(target)) {
                return false; // Don't target feared entities
            }
            return canUse;
        }

        @Override
        protected boolean canAttack(LivingEntity potentialTarget, TargetingConditions targetPredicate) {
            if (humanEntity.isAfraidOf(potentialTarget)) {
                return false; // Never attack feared entities
            }
            if(potentialTarget instanceof Creeper){
                return false;
            }
            return super.canAttack(potentialTarget, targetPredicate);
        }
    }

    public static class PickupMobDropsGoal extends Goal {
        private final AbstractMercenaryEntity entity;
        private ItemEntity targetItem;
        private int searchCooldown = 0;
        private int stuckTicks = 0;
        private Vec3 lastPosition;

        private static final int SEARCH_RADIUS = 16;
        private static final int SEARCH_COOLDOWN = 40; // 2 seconds between searches
        private static final int MAX_STUCK_TICKS = 100; // 5 seconds before giving up on stuck item
        private static final double PICKUP_DISTANCE_SQ = 4.0; // 2 blocks
        private static final double MOVEMENT_THRESHOLD = 0.1; // Minimum movement to not be considered stuck

        public PickupMobDropsGoal(AbstractMercenaryEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Don't run if entity is in combat
            if (entity.getTarget() != null) {
                return false;
            }

            // Don't run if entity is in standby mode
            if (entity.isPatrolMode()) {
                return false;
            }

            if(this.entity.isStunned()){
                return false;
            }

            if(this.entity.isInMenuState()){
                return false;
            }

            if (entity.isSittingInChair()) {
                return false;
            }

            // Apply search cooldown
            if (searchCooldown > 0) {
                searchCooldown--;
                return false;
            }

            // Only run if entity can pick up loot
            if (!entity.canPickUpLoot()) {
                return false;
            }

            // Find nearby item entities
            return findNearestPickupableItem();
        }

        @Override
        public boolean canContinueToUse() {
            // Stop if we now have a target to fight
            if (entity.getTarget() != null) {
                return false;
            }

            // Stop if entity goes into standby
            if (entity.isPatrolMode()) {
                return false;
            }

            // Stop if target item is gone or too far away
            if (targetItem == null || !targetItem.isAlive() ||
                    entity.distanceToSqr(targetItem) > SEARCH_RADIUS * SEARCH_RADIUS) {
                return false;
            }

            // Stop if we've been stuck for too long
            if (stuckTicks >= MAX_STUCK_TICKS) {
                return false;
            }

            // Stop if item is no longer worth picking up
            if (!isItemWorthPickingUp(targetItem)) {
                return false;
            }

            // Stop if we can no longer fit this item in our inventory
            if (!canPickupItem(targetItem)) {
                return false;
            }

            return true;
        }

        @Override
        public void start() {
            if (targetItem != null) {
                entity.getNavigation().moveTo(targetItem, 1.0);
                stuckTicks = 0;
                lastPosition = entity.position();
            }
        }

        @Override
        public void tick() {
            if (targetItem == null) {
                return;
            }

            double distanceToItem = entity.distanceToSqr(targetItem);

            // Check if we're close enough to pick up the item
            if (distanceToItem <= PICKUP_DISTANCE_SQ) {
                // The entity's built-in pickup logic should handle this
                // But we can trigger it manually if needed
                if (entity.wantsToPickUp(targetItem.getItem())) {
                    entity.take(targetItem, targetItem.getItem().getCount());
                }
                return;
            }

            // Check if we're stuck
            Vec3 currentPosition = entity.position();
            if (lastPosition != null) {
                double movementDistance = currentPosition.distanceTo(lastPosition);
                if (movementDistance < MOVEMENT_THRESHOLD) {
                    stuckTicks++;
                } else {
                    stuckTicks = 0; // Reset if we're moving
                }
            }
            lastPosition = currentPosition;

            // Try to navigate to the item
            if (entity.getNavigation().isDone() || stuckTicks > 20) {
                // Recalculate path if navigation is done or we've been stuck for a bit
                entity.getNavigation().moveTo(targetItem, 1.0);

                // If we're really stuck, try moving to a slightly different position
                if (stuckTicks > 40) {
                    double offsetX = (entity.getRandom().nextDouble() - 0.5) * 2.0;
                    double offsetZ = (entity.getRandom().nextDouble() - 0.5) * 2.0;
                    entity.getNavigation().moveTo(
                            targetItem.getX() + offsetX,
                            targetItem.getY(),
                            targetItem.getZ() + offsetZ,
                            1.0
                    );
                }
            }
        }

        @Override
        public void stop() {
            targetItem = null;
            searchCooldown = SEARCH_COOLDOWN;
            stuckTicks = 0;
            lastPosition = null;
            entity.getNavigation().stop();
        }

        private boolean canPickupItem(ItemEntity itemEntity) {
            if (itemEntity == null || !itemEntity.isAlive()) {
                return false;
            }

            ItemStack itemStack = itemEntity.getItem();
            if (itemStack.isEmpty()) {
                return false;
            }

            // Check if the inventory can add this item
            SimpleContainer inventory = entity.getInventory();
            return inventory.canAddItem(itemStack);
        }

        private boolean findNearestPickupableItem() {
            AABB searchArea = new AABB(entity.blockPosition()).inflate(SEARCH_RADIUS);
            List<ItemEntity> nearbyItems = entity.level().getEntitiesOfClass(ItemEntity.class, searchArea);

            ItemEntity closestItem = null;
            double closestDistance = Double.MAX_VALUE;

            for (ItemEntity itemEntity : nearbyItems) {
                // Skip if item is not worth picking up
                if (!isItemWorthPickingUp(itemEntity)) {
                    continue;
                }

                // Skip if entity doesn't want to pick up this item
                if (!entity.wantsToPickUp(itemEntity.getItem())) {
                    continue;
                }

                // Skip if we can't fit this item in our inventory
                if (!canPickupItem(itemEntity)) {
                    continue;
                }

                // Skip items that are too young (just dropped)
                if (itemEntity.getAge() < 20) { // 1 second old minimum
                    continue;
                }

                double distance = entity.distanceToSqr(itemEntity);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestItem = itemEntity;
                }
            }

            if (closestItem != null) {
                targetItem = closestItem;
                return true;
            }

            return false;
        }

        private boolean isItemWorthPickingUp(ItemEntity itemEntity) {
            if (itemEntity == null || !itemEntity.isAlive()) {
                return false;
            }

            // Don't pick up items that are about to despawn
            if (itemEntity.getAge() > 5400) { // 4.5 minutes (items despawn at 6000 ticks)
                return false;
            }

            return true;
        }
    }

    public static class AlliedHumanDefenseGoal extends TargetGoal {
        private final AbstractMercenaryEntity human;
        private LivingEntity targetMob;
        private int timestamp;
        private static final double HELP_RANGE = 16.0D;

        public AlliedHumanDefenseGoal(AbstractMercenaryEntity human) {
            super(human, false);
            this.human = human;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            // Don't help if we already have a target
            if (human.getTarget() != null) {
                return false;
            }

            if(this.human.isStunned()){
                return false;
            }

            if(this.human.isInMenuState()){
                return false;
            }

            if (this.human.isSittingInChair()) {
                return false;
            }

            // Look for allied humans in combat within range
            AABB searchArea = human.getBoundingBox().inflate(HELP_RANGE);
            List<AbstractMercenaryEntity> nearbyHumans = human.level().getEntitiesOfClass(
                    AbstractMercenaryEntity.class,
                    searchArea,
                    alliedHuman -> alliedHuman != human && // Not ourselves
                            human.isAlliedTo(alliedHuman) && // Allied to us
                            alliedHuman.getTarget() != null && // Has a target
                            alliedHuman.getTarget().isAlive() && // Target is alive
                            canHelpAgainst(alliedHuman.getTarget()) // We can help against this target
            );

            // Find the closest allied humans that needs help
            AbstractMercenaryEntity closestAlliedHuman = null;
            double closestDistance = Double.MAX_VALUE;

            for (AbstractMercenaryEntity alliedHuman : nearbyHumans) {
                double distance = human.distanceToSqr(alliedHuman);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestAlliedHuman = alliedHuman;
                }
            }

            if (closestAlliedHuman != null && closestAlliedHuman.getTarget() != null) {
                this.targetMob = closestAlliedHuman.getTarget();
                return true;
            }

            return false;
        }

        @Override
        public boolean canContinueToUse() {
            // Stop if target is no longer valid
            if (targetMob == null || !targetMob.isAlive()) {
                return false;
            }

            // Stop if we can no longer help against this target
            if (!canHelpAgainst(targetMob)) {
                return false;
            }

            // Stop if target is too far away
            if (human.distanceToSqr(targetMob) > HELP_RANGE * HELP_RANGE * 2) {
                return false;
            }

            // Stop if no allied human is still fighting this target
            AABB searchArea = human.getBoundingBox().inflate(HELP_RANGE * 1.5);
            List<AbstractMercenaryEntity> alliedHumans = human.level().getEntitiesOfClass(
                    AbstractMercenaryEntity.class,
                    searchArea,
                    alliedHuman -> alliedHuman != human &&
                            human.isAlliedTo(alliedHuman) &&
                            alliedHuman.getTarget() == targetMob
            );

            return !alliedHumans.isEmpty();
        }

        @Override
        public void start() {
            human.setTarget(this.targetMob);
            this.timestamp = human.tickCount;
            super.start();
        }

        @Override
        public void stop() {
            human.setTarget(null);
            this.targetMob = null;
            super.stop();
        }

        private boolean canHelpAgainst(LivingEntity target) {
            // Don't attack allied entities
            if (human.isAlliedTo(target)) {
                return false;
            }

            // Don't attack entities we can't see
            if (!human.hasLineOfSight(target)) {
                return false;
            }

            // Use the same targeting conditions as other combat goals
            TargetingConditions conditions = TargetingConditions.forCombat()
                    .range(HELP_RANGE)
                    .selector(entity -> entity instanceof LivingEntity &&
                            !human.isAlliedTo(entity) &&
                            entity.isAlive());

            return conditions.test(human, target);
        }
    }

    public static class HumanHurtByTargetGoal extends HurtByTargetGoal {
        private final AbstractMercenaryEntity human;
        private static final double ALERT_RANGE = 20.0D;

        public HumanHurtByTargetGoal(AbstractMercenaryEntity human) {
            super(human);
            this.human = human;
        }

        @Override
        public void start() {
            super.start();

            // Alert nearby allied humans when we get hurt
            if (human.getLastHurtByMob() != null) {
                alertAlliedHumans(human.getLastHurtByMob());
            }
        }

        private void alertAlliedHumans(LivingEntity attacker) {
            if (attacker == null || !attacker.isAlive()) {
                return;
            }

            AABB searchArea = human.getBoundingBox().inflate(ALERT_RANGE);
            List<AbstractMercenaryEntity> nearbyHumans = human.level().getEntitiesOfClass(
                    AbstractMercenaryEntity.class,
                    searchArea,
                    alliedHuman -> alliedHuman != human && // Not ourselves
                            human.isAlliedTo(alliedHuman) && // Allied to us
                            (alliedHuman.getTarget() == null || // No current target
                                    alliedHuman.getTarget() == attacker) // Or already targeting this attacker
            );

            for (AbstractMercenaryEntity alliedHuman : nearbyHumans) {
                // Only set target if they can attack this entity
                if (!alliedHuman.isAlliedTo(attacker) &&
                        alliedHuman.hasLineOfSight(attacker) &&
                        alliedHuman.getTarget() == null) {

                    alliedHuman.setTarget(attacker);

                    // Also set their last hurt by mob so they know who to be angry at
                    if (alliedHuman.getLastHurtByMob() == null) {
                        alliedHuman.setLastHurtByMob(attacker);
                    }
                }
            }
        }
    }

    public static class HumanWizardAttackGoal extends Goal {

        protected LivingEntity target;
        protected final double speedModifier;
        protected final int spellAttackIntervalMin;
        protected final int spellAttackIntervalMax;
        protected float spellcastingRange;
        protected float spellcastingRangeSqr;
        protected boolean shortCircuitTemp = false;

        protected boolean hasLineOfSight;
        protected int seeTime = 0;
        protected int strafeTime;
        protected boolean strafingClockwise;
        protected int spellAttackDelay = -1;
        protected int projectileCount;

        protected AbstractSpell singleUseSpell = SpellRegistry.none();
        protected int singleUseDelay;
        protected int singleUseLevel;

        protected boolean isFlying;
        protected boolean allowFleeing;
        protected int fleeCooldown;
        protected int flyingMovementTimer;
        protected Vec3 flyingTarget;
        protected int lastHurtTime = -1;

        protected final ArrayList<AbstractSpell> attackSpells = new ArrayList<>();
        protected final ArrayList<AbstractSpell> defenseSpells = new ArrayList<>();
        protected final ArrayList<AbstractSpell> movementSpells = new ArrayList<>();
        protected final ArrayList<AbstractSpell> supportSpells = new ArrayList<>();
        protected ArrayList<AbstractSpell> lastSpellCategory = attackSpells;

        protected float minSpellQuality = .1f;
        protected float maxSpellQuality = .4f;

        protected boolean drinksPotions;
        protected final PathfinderMob mob;
        protected final AbstractSpellCastingMob spellCastingMob;

        private Map<AbstractSpell, Holder<MobEffect>> buffs = Map.ofEntries(
                Map.entry(SpellRegistry.EVASION_SPELL.get(), MobEffectRegistry.EVASION),
                Map.entry(SpellRegistry.HEARTSTOP_SPELL.get(), MobEffectRegistry.HEARTSTOP),
                Map.entry(SpellRegistry.CHARGE_SPELL.get(), MobEffectRegistry.CHARGED),
                Map.entry(SpellRegistry.INVISIBILITY_SPELL.get(), MobEffectRegistry.TRUE_INVISIBILITY),
                Map.entry(SpellRegistry.OAKSKIN_SPELL.get(), MobEffectRegistry.OAKSKIN),
                Map.entry(SpellRegistry.HASTE_SPELL.get(), MobEffectRegistry.HASTENED),
                Map.entry(SpellRegistry.FROSTBITE_SPELL.get(), MobEffectRegistry.FROSTBITTEN_STRIKES),
                Map.entry(SpellRegistry.ABYSSAL_SHROUD_SPELL.get(), MobEffectRegistry.ABYSSAL_SHROUD),
                Map.entry(SpellRegistry.ECHOING_STRIKES_SPELL.get(), MobEffectRegistry.ECHOING_STRIKES),
                Map.entry(SpellRegistry.FORTIFY_SPELL.get(), MobEffectRegistry.FORTIFY),
                Map.entry(SpellRegistry.THUNDERSTORM_SPELL.get(), MobEffectRegistry.THUNDERSTORM),
                Map.entry(SpellRegistry.SPIDER_ASPECT_SPELL.get(), MobEffectRegistry.SPIDER_ASPECT)
        );
        private Map<AbstractSpell, Holder<MobEffect>> debuffs = Map.of(
                SpellRegistry.BLIGHT_SPELL.get(), MobEffectRegistry.BLIGHT,
                SpellRegistry.SLOW_SPELL.get(), MobEffectRegistry.SLOWED
        );

        public HumanWizardAttackGoal(AbstractSpellCastingMob abstractSpellCastingMob, double pSpeedModifier, int pAttackInterval) {
            this(abstractSpellCastingMob, pSpeedModifier, pAttackInterval, pAttackInterval);
        }

        public HumanWizardAttackGoal(AbstractSpellCastingMob abstractSpellCastingMob, double pSpeedModifier, int pAttackIntervalMin, int pAttackIntervalMax) {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Flag.TARGET));
            this.spellCastingMob = abstractSpellCastingMob;
            if (abstractSpellCastingMob instanceof PathfinderMob m) {
                this.mob = m;
            } else
                throw new IllegalStateException("Unable to add " + this.getClass().getSimpleName() + "to entity, must extend PathfinderMob.");

            this.speedModifier = pSpeedModifier;
            this.spellAttackIntervalMin = pAttackIntervalMin;
            this.spellAttackIntervalMax = pAttackIntervalMax;
            this.spellcastingRange = 20;
            this.spellcastingRangeSqr = spellcastingRange * spellcastingRange;
            allowFleeing = true;
            flyingMovementTimer = 0;
        }

        public HumanWizardAttackGoal setSpells(List<AbstractSpell> attackSpells, List<AbstractSpell> defenseSpells, List<AbstractSpell> movementSpells, List<AbstractSpell> supportSpells) {
            this.attackSpells.clear();
            this.defenseSpells.clear();
            this.movementSpells.clear();
            this.supportSpells.clear();

            this.attackSpells.addAll(attackSpells);
            this.defenseSpells.addAll(defenseSpells);
            this.movementSpells.addAll(movementSpells);
            this.supportSpells.addAll(supportSpells);

            return this;
        }

        public HumanWizardAttackGoal setSpellQuality(float minSpellQuality, float maxSpellQuality) {
            this.minSpellQuality = minSpellQuality;
            this.maxSpellQuality = maxSpellQuality;
            return this;
        }

        public HumanWizardAttackGoal setSingleUseSpell(AbstractSpell abstractSpell, int minDelay, int maxDelay, int minLevel, int maxLevel) {
            this.singleUseSpell = abstractSpell;
            this.singleUseDelay = Utils.random.nextIntBetweenInclusive(minDelay, maxDelay);
            this.singleUseLevel = Utils.random.nextIntBetweenInclusive(minLevel, maxLevel);
            return this;
        }

        public HumanWizardAttackGoal setIsFlying() {
            isFlying = true;
            return this;
        }

        public HumanWizardAttackGoal setDrinksPotions() {
            drinksPotions = true;
            return this;
        }

        public HumanWizardAttackGoal setAllowFleeing(boolean allowFleeing) {
            this.allowFleeing = allowFleeing;
            return this;
        }

        public boolean canUse() {
            if(this.mob instanceof AbstractMercenaryEntity human && (human.isStunned() || human.isSittingInChair() || human.isInMenuState())){
                return false;
            }
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity != null && livingentity.isAlive()) {
                this.target = livingentity;
                return mob.canAttack(target);
            } else {
                return false;
            }
        }

        public boolean canContinueToUse() {
            return this.canUse();
        }

        public void stop() {
            this.target = null;
            this.seeTime = 0;
            this.spellAttackDelay = -1;
            this.mob.setAggressive(false);
            this.mob.getMoveControl().strafe(0, 0);
            this.mob.getNavigation().stop();
            this.flyingTarget = null;
            this.flyingMovementTimer = 0;
            this.lastHurtTime = -1;
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public void tick() {
            if (target == null) {
                return;
            }

            if (target.isDeadOrDying()) {
                LivingEntity newTarget = findNearbyTarget();
                if (newTarget != null) {
                    this.target = newTarget;
                    this.mob.setTarget(newTarget);
                    this.seeTime = 0;
                    this.spellAttackDelay = Math.max(this.spellAttackDelay, 10);
                } else {
                    return;
                }
            }

            double distanceSquared = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
            hasLineOfSight = this.mob.getSensing().hasLineOfSight(this.target);
            if (hasLineOfSight) {
                this.seeTime++;
            } else {
                this.seeTime--;
            }

            //default mage movement
            doMovement(distanceSquared);

            //do attacks
            if (mob.getLastHurtByMobTimestamp() == mob.tickCount - 1) {
                spellAttackDelay = (int) (Mth.lerp(.6f, spellAttackDelay, 0) + 1);
                lastHurtTime = mob.tickCount;
            }

            //default attack timer
            handleAttackLogic(distanceSquared);

            singleUseDelay--;
            flyingMovementTimer--;
        }

        protected void handleAttackLogic(double distanceSquared) {
            if (seeTime < -50) {
                return;
            }
            if (--this.spellAttackDelay == 0) {
                resetSpellAttackTimer(distanceSquared);
                if (!spellCastingMob.isCasting() && !spellCastingMob.isDrinkingPotion()) {
                    doSpellAction();
                }

            } else if (this.spellAttackDelay < 0) {
                resetSpellAttackTimer(distanceSquared);
            }
            if (spellCastingMob.isCasting()) {
                var spellData = MagicData.getPlayerMagicData(mob).getCastingSpell();
                if (target.isDeadOrDying() || spellData.getSpell().shouldAIStopCasting(spellData.getLevel(), mob, target)) {
                    spellCastingMob.cancelCast();
                }
            }
        }

        public boolean isActing() {
            return spellCastingMob.isCasting() || spellCastingMob.isDrinkingPotion();
        }

        protected void resetSpellAttackTimer(double distanceSquared) {
            float f = (float) Math.sqrt(distanceSquared) / this.spellcastingRange;
            this.spellAttackDelay = Math.max(1, Mth.floor(f * (float) (this.spellAttackIntervalMax - this.spellAttackIntervalMin) + (float) this.spellAttackIntervalMin));
        }

        protected void doMovement(double distanceSquared) {
            double speed = (spellCastingMob.isCasting() ? .75f : 1f) * movementSpeed();

            if (target != null) {
                mob.lookAt(target, 30, 30);
                if (isFlying && spellCastingMob.isCasting()) {
                    forceLookAtTarget(target);
                }
            }

            if (isFlying) {
                doFlyingMovement(distanceSquared, speed);
            } else {
                doGroundMovement(distanceSquared, speed);
            }
        }

        protected void doFlyingMovement(double distanceSquared, double speed) {
            float fleeDist = .275f;

            // Fleeing movement
            if (allowFleeing && (!spellCastingMob.isCasting() && spellAttackDelay > 10) && --fleeCooldown <= 0 && distanceSquared < spellcastingRangeSqr * (fleeDist * fleeDist)) {
                Vec3 flee = DefaultRandomPos.getPosAway(this.mob, 16, 7, target.position());
                if (flee != null) {
                    flyingTarget = new Vec3(flee.x, flee.y + 3, flee.z);
                    flyingMovementTimer = 60;
                }
            }
            // In range movement
            else if (distanceSquared < spellcastingRangeSqr && seeTime >= 5) {
                boolean shouldGenerateNewTarget = !spellCastingMob.isCasting() &&
                        (flyingTarget == null || flyingMovementTimer <= 0 || mob.position().distanceTo(flyingTarget) < 2);

                if (spellCastingMob.isCasting() && mob.getRandom().nextInt(20) == 0) {
                    shouldGenerateNewTarget = true;
                }

                if (shouldGenerateNewTarget) {
                    double angle = mob.getRandom().nextDouble() * 2 * Math.PI;
                    double radius = 5 + mob.getRandom().nextDouble() * 10;
                    double x = target.getX() + Math.cos(angle) * radius;
                    double z = target.getZ() + Math.sin(angle) * radius;

                    double baseHeight = target.getY();
                    double heightVariation = (mob.getRandom().nextDouble() - 0.5) * 8;
                    double y = Math.max(baseHeight + 2, baseHeight + heightVariation + 3);

                    flyingTarget = new Vec3(x, y, z);
                    flyingMovementTimer = spellCastingMob.isCasting() ? 60 : 30 + mob.getRandom().nextInt(30);
                }

                if (flyingTarget != null) {
                    double flyingSpeed = spellCastingMob.isCasting() ? speed * 0.4 : speed;

                    Vec3 direction = flyingTarget.subtract(mob.position()).normalize();
                    Vec3 movement = direction.scale(flyingSpeed * 0.1);
                    mob.setDeltaMovement(movement);
                }
            }
            // Out of range movement
            else {
                if (mob.tickCount % 5 == 0 || flyingTarget == null) {
                    double targetY = target.getY() + 2 + mob.getRandom().nextDouble() * 3;
                    flyingTarget = new Vec3(target.getX(), targetY, target.getZ());
                    flyingMovementTimer = 20;
                }
            }
        }

        protected void doGroundMovement(double distanceSquared, double speed) {
            // Default movement
            float fleeDist = .275f;
            float ss = getStrafeMultiplier();
            if (allowFleeing && (!spellCastingMob.isCasting() && spellAttackDelay > 10) && --fleeCooldown <= 0 && distanceSquared < spellcastingRangeSqr * (fleeDist * fleeDist)) {
                Vec3 flee = DefaultRandomPos.getPosAway(this.mob, 16, 7, target.position());
                if (flee != null) {
                    this.mob.getNavigation().moveTo(flee.x, flee.y, flee.z, speed * 1.5);
                } else {
                    mob.getMoveControl().strafe(-(float) speed * ss, (float) speed * ss);
                }
            } else if (distanceSquared < spellcastingRangeSqr && seeTime >= 5) {
                this.mob.getNavigation().stop();
                if (++strafeTime > 25) {
                    if (mob.getRandom().nextDouble() < .1) {
                        strafingClockwise = !strafingClockwise;
                        strafeTime = 0;
                    }
                }
                float strafeForward = (distanceSquared * 6 < spellcastingRangeSqr ? -1 : .5f) * .2f * (float) speedModifier;
                int strafeDir = strafingClockwise ? 1 : -1;
                mob.getMoveControl().strafe(strafeForward * ss, (float) speed * strafeDir * ss);
                if (mob.horizontalCollision && mob.getRandom().nextFloat() < .1f) {
                    tryJump();
                }
            } else {
                if (mob.tickCount % 5 == 0) {
                    this.mob.getNavigation().moveTo(this.target, speedModifier);
                }
            }
        }

        protected double movementSpeed() {
            return speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2;
        }

        protected void tryJump() {
            Vec3 nextBlock = new Vec3(mob.xxa, 0, mob.zza).normalize();
            BlockPos blockpos = BlockPos.containing(mob.position().add(nextBlock));
            BlockState blockstate = this.mob.level().getBlockState(blockpos);
            VoxelShape voxelshape = blockstate.getCollisionShape(this.mob.level(), blockpos);
            if (!voxelshape.isEmpty() && !blockstate.is(BlockTags.DOORS) && !blockstate.is(BlockTags.FENCES)) {
                BlockPos blockposAbove = blockpos.above();
                BlockState blockstateAbove = this.mob.level().getBlockState(blockposAbove);
                VoxelShape voxelshapeAbove = blockstateAbove.getCollisionShape(this.mob.level(), blockposAbove);
                if (voxelshapeAbove.isEmpty()) {
                    this.mob.getJumpControl().jump();
                    mob.setXxa(mob.xxa * 5);
                    mob.setZza(mob.zza * 5);
                }
            }
        }

        protected void doSpellAction() {
            if (!spellCastingMob.getHasUsedSingleAttack() && singleUseSpell != SpellRegistry.none() && singleUseDelay <= 0) {
                spellCastingMob.setHasUsedSingleAttack(true);
                spellCastingMob.initiateCastSpell(singleUseSpell, singleUseLevel);
                fleeCooldown = 7 + singleUseSpell.getCastTime(singleUseLevel);
            } else {
                var spell = getNextSpellType();
                int spellLevel = (int) (spell.getMaxLevel() * Mth.lerp(mob.getRandom().nextFloat(), minSpellQuality, maxSpellQuality));
                spellLevel = Math.max(spellLevel, 1);

                if (!spell.shouldAIStopCasting(spellLevel, mob, target)) {
                    spellCastingMob.initiateCastSpell(spell, spellLevel);
                    fleeCooldown = 7 + spell.getCastTime(spellLevel);
                } else {
                    spellAttackDelay = 5;
                }
            }
        }

        protected AbstractSpell getNextSpellType() {
            NavigableMap<Integer, ArrayList<AbstractSpell>> weightedSpells = new TreeMap<>();
            int attackWeight = Math.max(0, getAttackWeight());
            int defenseWeight = Math.max(0, getDefenseWeight() - (lastSpellCategory == defenseSpells ? 100 : 0));
            int movementWeight = Math.max(0, getMovementWeight() - (lastSpellCategory == movementSpells ? 50 : 0));
            int supportWeight = Math.max(0, getSupportWeight() - (lastSpellCategory == supportSpells ? 100 : 0));
            int total = 0;

            if (!attackSpells.isEmpty() && attackWeight > 0) {
                total += attackWeight;
                weightedSpells.put(total, getFilteredAttackSpells());
            }
            if (!defenseSpells.isEmpty() && defenseWeight > 0) {
                total += defenseWeight;
                weightedSpells.put(total, getFilteredDefenseSpells());
            }
            if (!movementSpells.isEmpty() && movementWeight > 0) {
                total += movementWeight;
                weightedSpells.put(total, getFilteredMovementSpells());
            }
            if ((!supportSpells.isEmpty() || drinksPotions) && supportWeight > 0) {
                total += supportWeight;
                weightedSpells.put(total, getFilteredSupportSpells());
            }

            // Safety check: if total is still 0, fallback to attack spells
            if (total <= 0) {
                if (!attackSpells.isEmpty()) {
                    lastSpellCategory = attackSpells;
                    return attackSpells.get(mob.getRandom().nextInt(attackSpells.size()));
                } else if (!defenseSpells.isEmpty()) {
                    lastSpellCategory = defenseSpells;
                    return defenseSpells.get(mob.getRandom().nextInt(defenseSpells.size()));
                } else if (!movementSpells.isEmpty()) {
                    lastSpellCategory = movementSpells;
                    return movementSpells.get(mob.getRandom().nextInt(movementSpells.size()));
                } else if (!supportSpells.isEmpty()) {
                    lastSpellCategory = supportSpells;
                    return supportSpells.get(mob.getRandom().nextInt(supportSpells.size()));
                } else {
                    // No spells available at all
                    return SpellRegistry.none();
                }
            }

            int seed = mob.getRandom().nextInt(total);
            var spellList = weightedSpells.higherEntry(seed).getValue();
            lastSpellCategory = spellList;

            if (drinksPotions && spellList == supportSpells) {
                if (supportSpells.isEmpty() || mob.getRandom().nextFloat() < .5f) {
                    spellCastingMob.startDrinkingPotion();
                    return SpellRegistry.none();
                }
            }

            // Safety check for empty spell list
            if (spellList.isEmpty()) {
                return SpellRegistry.none();
            }

            return spellList.get(mob.getRandom().nextInt(spellList.size()));
        }

        protected ArrayList<AbstractSpell> getFilteredAttackSpells() {
            if (target == null) return new ArrayList<>(attackSpells);

            double distance = Math.sqrt(mob.distanceToSqr(target));

            // Choose priority
            List<AbstractSpell> rangeSpells = new ArrayList<>();
            if (distance <= 3) {
                rangeSpells = filterSpellsByTags(attackSpells, ModTags.CLOSE_RANGE_ATTACKS);
                if (rangeSpells.isEmpty()) {
                    rangeSpells = filterSpellsByTags(attackSpells, ModTags.MID_RANGE_ATTACKS);
                    if (rangeSpells.isEmpty()) {
                        rangeSpells = filterSpellsByTags(attackSpells, ModTags.LONG_RANGE_ATTACKS);
                    }
                }
            } else if (distance <= 6) {
                rangeSpells = filterSpellsByTags(attackSpells, ModTags.MID_RANGE_ATTACKS);
                if (rangeSpells.isEmpty()) {
                    rangeSpells = filterSpellsByTags(attackSpells, ModTags.LONG_RANGE_ATTACKS);
                    if (rangeSpells.isEmpty()) {
                        rangeSpells = filterSpellsByTags(attackSpells, ModTags.CLOSE_RANGE_ATTACKS);
                    }
                }
            } else {
                rangeSpells = filterSpellsByTags(attackSpells, ModTags.LONG_RANGE_ATTACKS);
                if (rangeSpells.isEmpty()) {
                    rangeSpells = filterSpellsByTags(attackSpells, ModTags.MID_RANGE_ATTACKS);
                    if (rangeSpells.isEmpty()) {
                        rangeSpells = filterSpellsByTags(attackSpells, ModTags.CLOSE_RANGE_ATTACKS);
                    }
                }
            }

            if (rangeSpells.isEmpty()) {
                rangeSpells = new ArrayList<>(attackSpells);
            }

            int entitiesNearTarget = getEntitiesNearTarget();
            List<AbstractSpell> finalSpells = new ArrayList<>();

            if (entitiesNearTarget >= 2) {
                // Priority AOE
                finalSpells = filterSpellsByTags(rangeSpells, ModTags.AOE_ATTACKS);
                if (finalSpells.isEmpty()) {
                    finalSpells = filterSpellsByTags(rangeSpells, ModTags.SINGLE_TARGET_ATTACKS);
                }
            } else {
                // Priority Single Target
                finalSpells = filterSpellsByTags(rangeSpells, ModTags.SINGLE_TARGET_ATTACKS);
                if (finalSpells.isEmpty()) {
                    finalSpells = filterSpellsByTags(rangeSpells, ModTags.AOE_ATTACKS);
                }
            }

            return finalSpells.isEmpty() ? new ArrayList<>(rangeSpells) : new ArrayList<>(finalSpells);
        }

        protected ArrayList<AbstractSpell> getFilteredDefenseSpells() {
            List<AbstractSpell> filteredSpells = new ArrayList<>();

            int timeSinceHurt = mob.tickCount - lastHurtTime;
            if (lastHurtTime == -1 || timeSinceHurt > 100) {
                return new ArrayList<>();
            }

            if (timeSinceHurt < 20) {
                return new ArrayList<>();
            }

            boolean hasCloseEnemies = hasEntitiesInRange(3);

            if (hasCloseEnemies) {
                filteredSpells = filterSpellsByTags(defenseSpells, ModTags.COUNTERATTACK_DEFENSE);

                if (filteredSpells.isEmpty()) {
                    List<AbstractSpell> escapeSpells = filterSpellsByTags(movementSpells, ModTags.RETREAT_MOVEMENT);
                    if (!escapeSpells.isEmpty()) {
                        filteredSpells = escapeSpells;
                    } else {
                        List<AbstractSpell> selfBuffSpells = filterSpellsByTags(defenseSpells, ModTags.SELF_BUFF_DEFENSE);
                        List<AbstractSpell> availableSelfBuffs = filterSpellsWithoutExistingBuffs(selfBuffSpells, mob);

                        if (!availableSelfBuffs.isEmpty()) {
                            filteredSpells = availableSelfBuffs;
                        }
                    }
                }
            } else {
                List<AbstractSpell> selfBuffSpells = filterSpellsByTags(defenseSpells, ModTags.SELF_BUFF_DEFENSE);
                List<AbstractSpell> availableSelfBuffs = filterSpellsWithoutExistingBuffs(selfBuffSpells, mob);

                if (!availableSelfBuffs.isEmpty()) {
                    filteredSpells = availableSelfBuffs;
                } else {
                    filteredSpells = filterSpellsByTags(defenseSpells, ModTags.COUNTERATTACK_DEFENSE);
                }
            }

            if(mob.getHealth() >= mob.getMaxHealth()){
                filteredSpells.remove(SpellRegistry.HEAL_SPELL.get());
                filteredSpells.remove(SpellRegistry.GREATER_HEAL_SPELL.get());
            }

            return filteredSpells.isEmpty() ? new ArrayList<>() : new ArrayList<>(filteredSpells);
        }

        protected ArrayList<AbstractSpell> getFilteredMovementSpells() {
            if (target == null) return new ArrayList<>(movementSpells);

            double targetDistance = Math.sqrt(mob.distanceToSqr(target));
            boolean hasCloseHostiles = hasHostileEntitiesInRange(3);

            List<AbstractSpell> filteredSpells = new ArrayList<>();

            if (hasCloseHostiles) {
                filteredSpells = filterSpellsByTags(movementSpells, ModTags.RETREAT_MOVEMENT);
                if (filteredSpells.isEmpty()) {
                    filteredSpells = filterSpellsByTags(movementSpells, ModTags.APPROACH_MOVEMENT);
                }
            } else if (targetDistance > 5) {
                filteredSpells = filterSpellsByTags(movementSpells, ModTags.APPROACH_MOVEMENT);
                if (filteredSpells.isEmpty()) {
                    filteredSpells = filterSpellsByTags(movementSpells, ModTags.RETREAT_MOVEMENT);
                }
            } else {
                filteredSpells = filterSpellsByTags(movementSpells, ModTags.APPROACH_MOVEMENT);
                if (filteredSpells.isEmpty()) {
                    filteredSpells = filterSpellsByTags(movementSpells, ModTags.RETREAT_MOVEMENT);
                }
            }

            return filteredSpells.isEmpty() ? new ArrayList<>(movementSpells) : new ArrayList<>(filteredSpells);
        }

        protected ArrayList<AbstractSpell> getFilteredSupportSpells() {
            float healthPercentage = mob.getHealth() / mob.getMaxHealth();
            List<AbstractSpell> filteredSpells = new ArrayList<>();

            if (healthPercentage > 0.5f) {
                // More than 50% health
                List<AbstractSpell> safeBuffs = filterSpellsByTags(supportSpells, ModTags.UNTHREATENED_BUFF_BUFFING);
                List<AbstractSpell> availableSafeBuffs = filterSpellsWithoutExistingBuffs(safeBuffs, mob);

                List<AbstractSpell> debuffs = filterSpellsByTags(supportSpells, ModTags.DEBUFF_BUFFING);
                List<AbstractSpell> availableDebuffs = filterSpellsWithoutExistingDebuffs(debuffs, target);

                filteredSpells.addAll(availableSafeBuffs);
                filteredSpells.addAll(availableDebuffs);
            } else {
                // Less than 50% health
                List<AbstractSpell> unsafeBuffs = filterSpellsByTags(supportSpells, ModTags.THREATENED_BUFF_BUFFING);
                List<AbstractSpell> availableUnsafeBuffs = filterSpellsWithoutExistingBuffs(unsafeBuffs, mob);

                List<AbstractSpell> debuffs = filterSpellsByTags(supportSpells, ModTags.DEBUFF_BUFFING);
                List<AbstractSpell> availableDebuffs = filterSpellsWithoutExistingDebuffs(debuffs, target);

                // Unsafe buffs have more chance (only if available)
                for (int i = 0; i < 3; i++) {
                    filteredSpells.addAll(availableUnsafeBuffs);
                }
                filteredSpells.addAll(availableDebuffs);
            }

            if(mob.getHealth() >= mob.getMaxHealth()){
                filteredSpells.remove(SpellRegistry.HEAL_SPELL.get());
            }

            if(!hasHarmfulEffects(mob)){
                filteredSpells.remove(SpellRegistry.CLEANSE_SPELL.get());
            }

            return filteredSpells.isEmpty() ? new ArrayList<>(supportSpells) : new ArrayList<>(filteredSpells);
        }

        public static boolean hasHarmfulEffects(LivingEntity entity) {
            for (MobEffectInstance effect : entity.getActiveEffects()) {
                Holder<MobEffect> mobEffect = effect.getEffect();

                if (!mobEffect.value().isBeneficial()) {
                    return true;
                }
            }
            return false;
        }

        protected List<AbstractSpell> filterSpellsByTags(List<AbstractSpell> spells, TagKey<AbstractSpell> tag) {
            var list = new ArrayList<AbstractSpell>();

            for (var spell : spells) {
                SpellRegistry.REGISTRY.getHolder(spell.getSpellResource()).ifPresent(a -> {
                    if (a.is(tag)) {
                        list.add(spell);
                    }
                });
            }

            return list;
        }

        protected List<AbstractSpell> filterSpellsWithoutExistingBuffs(List<AbstractSpell> spells, LivingEntity entity) {
            if (entity == null) return new ArrayList<>(spells);

            List<AbstractSpell> availableSpells = new ArrayList<>();

            for (AbstractSpell spell : spells) {
                Holder<MobEffect> effect = buffs.get(spell);
                if (effect == null || !entity.hasEffect(effect)) {
                    availableSpells.add(spell);
                }
            }

            return availableSpells;
        }

        protected List<AbstractSpell> filterSpellsWithoutExistingDebuffs(List<AbstractSpell> spells, LivingEntity targetEntity) {
            if (targetEntity == null) return new ArrayList<>(spells);

            List<AbstractSpell> availableSpells = new ArrayList<>();

            for (AbstractSpell spell : spells) {
                if(spell == SpellRegistry.ROOT_SPELL.get()){
                    if(!target.getType().is(io.redspace.ironsspellbooks.util.ModTags.CANT_ROOT)){
                        availableSpells.add(spell);
                    }
                } else {
                    Holder<MobEffect> effect = debuffs.get(spell);
                    if (effect == null || !targetEntity.hasEffect(effect)) {
                        availableSpells.add(spell);
                    }
                }
            }

            return availableSpells;
        }

        protected int getEntitiesNearTarget() {
            if (target == null) return 0;

            AABB area = target.getBoundingBox().inflate(3.0);
            return mob.level().getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != target && entity != mob && entity.isAlive()).size();
        }

        protected boolean hasEntitiesInRange(double range) {
            AABB area = mob.getBoundingBox().inflate(range);
            return !mob.level().getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != mob && entity.isAlive()).isEmpty();
        }

        protected boolean hasHostileEntitiesInRange(double range) {
            AABB area = mob.getBoundingBox().inflate(range);
            return !mob.level().getEntitiesOfClass(Mob.class, area,
                    entity -> entity != mob && entity.isAlive() &&
                            (entity instanceof Enemy || entity.getTarget() == mob)).isEmpty();
        }

        @Override
        public void start() {
            super.start();
            this.mob.setAggressive(true);
        }

        protected int getAttackWeight() {
            int baseWeight = 80;
            if (!hasLineOfSight || target == null) {
                return 0;
            }

            float targetHealth = target.getHealth() / target.getMaxHealth();
            int targetHealthWeight = (int) ((1 - targetHealth) * baseWeight * .75f);

            double distanceSquared = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
            int distanceWeight = (int) (1 - (distanceSquared / spellcastingRangeSqr) * -60);

            int result = baseWeight + targetHealthWeight + distanceWeight;
            return Math.max(0, result); // Ensure never negative
        }

        protected int getDefenseWeight() {
            int baseWeight = -20;

            if (target == null) {
                return 0;
            }

            int timeSinceHurt = mob.tickCount - lastHurtTime;
            if (lastHurtTime == -1 || timeSinceHurt > 100 || timeSinceHurt < 20) {
                return 0;
            }

            float x = mob.getHealth();
            float m = mob.getMaxHealth();
            int healthWeight = (int) (50 * (-(x * x * x) / (m * m * m) + 1));

            float targetHealth = target.getHealth() / target.getMaxHealth();
            int targetHealthWeight = (int) (1 - targetHealth) * -35;

            int threatWeight = projectileCount * 95;

            int recentAttackBonus = 150;

            int result = baseWeight + healthWeight + targetHealthWeight + threatWeight + recentAttackBonus;
            return Math.max(0, result); // Ensure never negative
        }

        protected int getMovementWeight() {
            if (target == null) {
                return 0;
            }

            double distanceSquared = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
            double distancePercent = Mth.clamp(distanceSquared / spellcastingRangeSqr, 0, 1);

            int distanceWeight = (int) ((distancePercent) * 50);
            int losWeight = hasLineOfSight ? 0 : 80;

            float healthInverted = 1 - mob.getHealth() / mob.getMaxHealth();
            float distanceInverted = (float) (1 - distancePercent);
            int runWeight = (int) (400 * healthInverted * healthInverted * distanceInverted * distanceInverted);

            int result = distanceWeight + losWeight + runWeight;
            return Math.max(0, result); // Ensure never negative
        }

        protected int getSupportWeight() {
            int baseWeight = -15;

            if (target == null) {
                return 0;
            }

            float health = 1 - mob.getHealth() / mob.getMaxHealth();
            int healthWeight = (int) (200 * health);

            double distanceSquared = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
            double distancePercent = Mth.clamp(distanceSquared / spellcastingRangeSqr, 0, 1);
            int distanceWeight = (int) ((1 - distancePercent) * -75);

            int result = baseWeight + healthWeight + distanceWeight;
            return Math.max(0, result); // Ensure never negative
        }

        @Override
        public boolean isInterruptable() {
            return !isActing();
        }

        public float getStrafeMultiplier(){
            return 1f;
        }

        protected void forceLookAtTarget(LivingEntity target) {
            if (target != null) {
                double d0 = target.getX() - this.mob.getX();
                double d2 = target.getZ() - this.mob.getZ();
                double d1 = target.getEyeY() - this.mob.getEyeY();

                double d3 = Math.sqrt(d0 * d0 + d2 * d2);
                float f = (float) (Mth.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
                float f1 = (float) (-(Mth.atan2(d1, d3) * (double) (180F / (float) Math.PI)));
                this.mob.setXRot(f1 % 360);
                this.mob.setYRot(f % 360);
            }
        }

        protected LivingEntity findNearbyTarget() {
            if (mob == null) return null;

            AABB searchArea = mob.getBoundingBox().inflate(10.0);
            List<LivingEntity> nearbyHostiles = mob.level().getEntitiesOfClass(
                    LivingEntity.class,
                    searchArea,
                    entity -> isValidTargetForContinuation(entity)
            );

            if (nearbyHostiles.isEmpty()) {
                return null;
            }

            return findPriorityTarget(nearbyHostiles);
        }

        protected boolean isValidTargetForContinuation(LivingEntity entity) {
            if (entity == null || entity == mob || entity.isDeadOrDying()) {
                return false;
            }

            if (mob instanceof AbstractMercenaryEntity human && human.isAlliedTo(entity)) {
                return false;
            }

            if (entity instanceof Mob hostileMob) {
                LivingEntity hostileTarget = hostileMob.getTarget();
                if (hostileTarget == null) {
                    return false;
                }

                if (hostileTarget == mob) {
                    return true;
                }

                if (mob instanceof AbstractMercenaryEntity human && human.isAlliedTo(hostileTarget)) {
                    return true;
                }

                return false;
            }

            return false;
        }

        protected LivingEntity findPriorityTarget(List<LivingEntity> potentialTargets) {
            if (mob instanceof AbstractMercenaryEntity human) {
                if (human.getSummoner() == null) {
                    return null;
                }

                for (LivingEntity entity : potentialTargets) {
                    if (entity instanceof Mob hostileMob) {
                        LivingEntity hostileTarget = hostileMob.getTarget();
                        if (hostileTarget == null) {
                            continue;
                        }

                        if (hostileTarget == mob) {
                            return entity;
                        }

                        if (human.isAlliedTo(hostileTarget)) {
                            return entity;
                        }
                    }
                }
            }
            return null;
        }
    }

    public static class HumanFollowOwnerGoal extends Goal {
        private final AbstractMercenaryEntity humanEntity;
        @Nullable
        private Entity owner;
        private Supplier<Entity> ownerGetter;
        private final double speedModifier;
        private final PathNavigation navigation;
        private int timeToRecalcPath;
        private final float stopDistance;
        private final float startDistance;
        private float oldWaterCost;
        private float teleportDistance;
        private boolean canFly;

        public HumanFollowOwnerGoal(AbstractMercenaryEntity humanEntity, Supplier<Entity> ownerGetter, double speedModifier,
                                    float startDistance, float stopDistance, boolean canFly, float teleportDistance) {
            this.humanEntity = humanEntity;
            this.ownerGetter = ownerGetter;
            this.speedModifier = speedModifier;
            this.navigation = humanEntity.getNavigation();
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            this.teleportDistance = teleportDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
            this.canFly = canFly;
        }

        @Override
        public boolean canUse() {
            // Don't follow if in patrol mode
            if (humanEntity.isPatrolMode()) {
                return false;
            }

            if(this.humanEntity.isStunned()){
                return false;
            }

            if (this.humanEntity.isSittingInChair()) {
                return false;
            }

            if(this.humanEntity.isInMenuState()){
                return false;
            }

            Entity livingentity = this.ownerGetter.get();
            if (livingentity == null) {
                return false;
            }

            if (!livingentity.isAlive()) {
                return false;
            }

            if (this.humanEntity.distanceToSqr(livingentity) < (double) (this.startDistance * this.startDistance)) {
                return false;
            } else {
                this.owner = livingentity;
                return true;
            }
        }

        @Override
        public boolean canContinueToUse() {
            // Don't follow if in patrol mode
            if (humanEntity.isPatrolMode()) {
                return false;
            }

            // Check if owner is still valid
            Entity currentOwner = this.ownerGetter.get();
            if (currentOwner == null || !currentOwner.isAlive()) {
                return false;
            }

            // Update owner reference
            this.owner = currentOwner;

            if (this.navigation.isDone()) {
                return false;
            } else {
                return !(this.humanEntity.distanceToSqr(this.owner) <= (double) (this.stopDistance * this.stopDistance));
            }
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
            this.oldWaterCost = this.humanEntity.getPathfindingMalus(PathType.WATER);
            this.humanEntity.setPathfindingMalus(PathType.WATER, 0.0F);
        }

        @Override
        public void stop() {
            this.owner = null;
            this.navigation.stop();
            this.humanEntity.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
        }

        public void tick() {
            // Additional safety check
            Entity currentOwner = this.ownerGetter.get();
            if (currentOwner == null || !currentOwner.isAlive()) {
                // Force stop the goal
                return;
            }

            this.owner = currentOwner; // Update reference

            boolean flag = this.shouldTryTeleportToOwner();
            if (!flag) {
                this.humanEntity.getLookControl().setLookAt(this.owner, 10.0F, (float) this.humanEntity.getMaxHeadXRot());
            }

            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = this.adjustedTickDelay(10);
                if (flag) {
                    this.tryToTeleportToOwner();
                } else {
                    if (false && canFly && !humanEntity.onGround()) {
                        Vec3 vec3 = owner.position();
                        this.humanEntity.getMoveControl().setWantedPosition(vec3.x, vec3.y + 2, vec3.z, this.speedModifier);
                    } else {
                        this.navigation.moveTo(this.owner, this.speedModifier);
                    }
                }
            }
        }

        public void tryToTeleportToOwner() {
            Entity livingentity = this.ownerGetter.get();
            if (livingentity != null && livingentity.isAlive()) {
                this.teleportToAroundBlockPos(livingentity.blockPosition());
            }
        }

        public boolean shouldTryTeleportToOwner() {
            Entity livingentity = this.ownerGetter.get();
            return livingentity != null &&
                    livingentity.isAlive() &&
                    humanEntity.distanceToSqr(livingentity) >= teleportDistance * teleportDistance;
        }

        private void teleportToAroundBlockPos(BlockPos pPos) {
            for (int i = 0; i < 10; i++) {
                int j = humanEntity.getRandom().nextIntBetweenInclusive(-3, 3);
                int k = humanEntity.getRandom().nextIntBetweenInclusive(-3, 3);
                if (Math.abs(j) >= 2 || Math.abs(k) >= 2) {
                    int l = humanEntity.getRandom().nextIntBetweenInclusive(-1, 1);
                    if (this.maybeTeleportTo(pPos.getX() + j, pPos.getY() + l, pPos.getZ() + k)) {
                        return;
                    }
                }
            }
        }

        private boolean maybeTeleportTo(int pX, int pY, int pZ) {
            if (!this.canTeleportTo(new BlockPos(pX, pY, pZ))) {
                return false;
            } else {
                humanEntity.moveTo((double) pX + 0.5, (double) pY, (double) pZ + 0.5, humanEntity.getYRot(), humanEntity.getXRot());
                this.navigation.stop();
                return true;
            }
        }

        private boolean canTeleportTo(BlockPos pPos) {
            PathType pathtype = WalkNodeEvaluator.getPathTypeStatic(humanEntity, pPos);
            if (pathtype != PathType.WALKABLE) {
                return false;
            } else {
                BlockState blockstate = humanEntity.level().getBlockState(pPos.below());
                if (!this.canFly && blockstate.getBlock() instanceof LeavesBlock) {
                    return false;
                } else {
                    BlockPos blockpos = pPos.subtract(humanEntity.blockPosition());
                    return humanEntity.level().noCollision(humanEntity, humanEntity.getBoundingBox().move(blockpos));
                }
            }
        }
    }

    public static class PatrolAroundPositionGoal extends Goal {
        private final AbstractMercenaryEntity entity;
        private final double speedModifier;
        private final int patrolRadius;
        private BlockPos targetPos;
        private int patrolCooldown = 0;
        private int stuckTicks = 0;
        private Vec3 lastPosition;

        private static final int PATROL_INTERVAL = 100; // 5 seconds between patrol moves
        private static final int MAX_STUCK_TICKS = 60; // 3 seconds before trying new position
        private static final double MOVEMENT_THRESHOLD = 0.1; // Minimum movement to not be considered stuck
        private static final double ARRIVAL_DISTANCE = 2.0; // How close to get to target position

        public PatrolAroundPositionGoal(AbstractMercenaryEntity entity, double speedModifier, int patrolRadius) {
            this.entity = entity;
            this.speedModifier = speedModifier;
            this.patrolRadius = patrolRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Only patrol if in patrol mode and not in combat
            if (!entity.isPatrolMode()) {
                return false;
            }

            if(this.entity.isStunned()){
                return false;
            }

            if (entity.isSittingInChair()) {
                return false;
            }

            if(this.entity.isInMenuState()){
                return false;
            }

            if (entity.getTarget() != null) {
                return false; // Don't patrol while in combat
            }

            if (patrolCooldown > 0) {
                patrolCooldown--;
                return false;
            }

            // Check if we have a valid patrol position
            BlockPos patrolCenter = entity.getPatrolPosition();
            if (patrolCenter == null || patrolCenter.equals(BlockPos.ZERO)) {
                // Set current position as patrol center if none set
                entity.setPatrolPosition(entity.blockPosition());
                patrolCenter = entity.getPatrolPosition();
            }

            // Generate a new patrol target position
            return generatePatrolTarget(patrolCenter);
        }

        @Override
        public boolean canContinueToUse() {
            // Stop if no longer in patrol mode or entered combat
            if (!entity.isPatrolMode() || entity.getTarget() != null) {
                return false;
            }

            // Stop if we've reached our target
            if (targetPos != null && entity.distanceToSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ()) <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
                return false;
            }

            // Stop if we've been stuck for too long
            if (stuckTicks >= MAX_STUCK_TICKS) {
                return false;
            }

            return targetPos != null;
        }

        @Override
        public void start() {
            if (targetPos != null) {
                entity.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speedModifier);
                stuckTicks = 0;
                lastPosition = entity.position();
            }
        }

        @Override
        public void tick() {
            if (targetPos == null) {
                return;
            }

            // Check if we're stuck
            Vec3 currentPosition = entity.position();
            if (lastPosition != null) {
                double movementDistance = currentPosition.distanceTo(lastPosition);
                if (movementDistance < MOVEMENT_THRESHOLD) {
                    stuckTicks++;
                } else {
                    stuckTicks = 0; // Reset if we're moving
                }
            }
            lastPosition = currentPosition;

            // Try to navigate to the target
            if (entity.getNavigation().isDone() || stuckTicks > 20) {
                // Recalculate path if navigation is done or we've been stuck for a bit
                entity.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speedModifier);

                // If we're really stuck, try a different position
                if (stuckTicks > 40) {
                    BlockPos patrolCenter = entity.getPatrolPosition();
                    if (patrolCenter != null) {
                        generatePatrolTarget(patrolCenter);
                        if (targetPos != null) {
                            entity.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speedModifier);
                        }
                    }
                }
            }
        }

        @Override
        public void stop() {
            targetPos = null;
            patrolCooldown = PATROL_INTERVAL;
            stuckTicks = 0;
            lastPosition = null;
            entity.getNavigation().stop();
        }

        private boolean generatePatrolTarget(BlockPos patrolCenter) {
            // Try to find a valid patrol position within the radius
            for (int attempts = 0; attempts < 10; attempts++) {
                double angle = entity.getRandom().nextDouble() * 2 * Math.PI;
                int distance = 3 + entity.getRandom().nextInt(patrolRadius - 2); // Between 3 and patrolRadius blocks

                int xOffset = (int) (Math.cos(angle) * distance);
                int zOffset = (int) (Math.sin(angle) * distance);

                BlockPos candidatePos = patrolCenter.offset(xOffset, 0, zOffset);

                // Find a suitable Y level (ground level)
                candidatePos = findGroundLevel(candidatePos);

                // Check if the position is valid and pathable
                if (isValidPatrolPosition(candidatePos, patrolCenter)) {
                    targetPos = candidatePos;
                    return true;
                }
            }

            // If we can't find a valid position, just stay near the patrol center
            targetPos = findGroundLevel(patrolCenter.offset(
                    entity.getRandom().nextInt(5) - 2,
                    0,
                    entity.getRandom().nextInt(5) - 2
            ));

            return targetPos != null;
        }

        private BlockPos findGroundLevel(BlockPos startPos) {
            Level level = entity.level();

            // Search down for ground
            for (int y = startPos.getY(); y >= startPos.getY() - 5; y--) {
                BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
                BlockPos abovePos = checkPos.above();

                // Check if this is a valid ground position
                if (!level.getBlockState(checkPos).isAir() &&
                        level.getBlockState(abovePos).isAir() &&
                        level.getBlockState(abovePos.above()).isAir()) {
                    return abovePos;
                }
            }

            // Search up for ground if we didn't find any below
            for (int y = startPos.getY() + 1; y <= startPos.getY() + 5; y++) {
                BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
                BlockPos abovePos = checkPos.above();

                if (!level.getBlockState(checkPos).isAir() &&
                        level.getBlockState(abovePos).isAir() &&
                        level.getBlockState(abovePos.above()).isAir()) {
                    return abovePos;
                }
            }

            // If all else fails, return the original position
            return startPos;
        }

        private boolean isValidPatrolPosition(BlockPos pos, BlockPos patrolCenter) {
            if (pos == null) return false;

            Level level = entity.level();

            // Check if position is within patrol radius
            double distanceFromCenter = Math.sqrt(pos.distSqr(patrolCenter));
            if (distanceFromCenter > patrolRadius) {
                return false;
            }

            // Check if the position has solid ground and space above
            BlockState groundState = level.getBlockState(pos.below());
            BlockState posState = level.getBlockState(pos);
            BlockState aboveState = level.getBlockState(pos.above());

            return !groundState.isAir() && // Has solid ground
                    posState.isAir() &&     // Position is air
                    aboveState.isAir() &&   // Space above is air
                    groundState.isSolid();  // Ground is solid
        }
    }

    public static class ChairSittingGoal extends Goal {
        private final AbstractMercenaryEntity entity;
        private BlockPos targetChair;
        private int searchCooldown = 0;
        private int stuckTicks = 0;
        private Vec3 lastPosition;
        private boolean wasManuallyUnseated = false; // Track if entity was manually unseated

        private static final int SEARCH_RADIUS = 16;
        private static final int SEARCH_COOLDOWN = 100; // 5 seconds between searches
        private static final int MANUAL_UNSEAT_COOLDOWN = 600; // 30 seconds after manual unseating
        private static final int MAX_STUCK_TICKS = 60; // 3 seconds before giving up
        private static final double MOVEMENT_THRESHOLD = 0.1;
        private static final double CHAIR_REACH_DISTANCE = 2.0;

        public ChairSittingGoal(AbstractMercenaryEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // Don't try to sit if already sitting
            if (entity.isSittingInChair()) {
                return false;
            }

            // Don't sit if on cooldown (this now includes manual unseat cooldown)
            if (!entity.canSitInChair()) {
                return false;
            }

            // Don't sit if in combat
            if (entity.getTarget() != null) {
                return false;
            }

            // Don't sit if stunned
            if (entity.isStunned()) {
                return false;
            }

            if(this.entity.isInMenuState()){
                return false;
            }

            // Don't sit if in patrol mode (they're busy)
            if (entity.isPatrolMode()) {
                return false;
            }

            // Apply search cooldown
            if (searchCooldown > 0) {
                searchCooldown--;
                return false;
            }

            // Only try to sit sometimes (20% chance when conditions are met)
            if (entity.getRandom().nextFloat() > 0.2f) {
                searchCooldown = SEARCH_COOLDOWN;
                return false;
            }

            // Find a nearby empty chair
            return findNearbyEmptyChair();
        }

        @Override
        public boolean canContinueToUse() {
            // Stop if we started sitting
            if (entity.isSittingInChair()) {
                return false;
            }

            // Stop if we entered combat
            if (entity.getTarget() != null) {
                return false;
            }

            // Stop if chair is no longer valid
            if (targetChair == null || !isChairAvailable(targetChair)) {
                return false;
            }

            // Stop if we've been stuck for too long
            if (stuckTicks >= MAX_STUCK_TICKS) {
                return false;
            }

            return true;
        }

        @Override
        public void start() {
            if (targetChair != null) {
                entity.getNavigation().moveTo(targetChair.getX(), targetChair.getY(), targetChair.getZ(), 1.0);
                stuckTicks = 0;
                lastPosition = entity.position();
            }
        }

        @Override
        public void stop() {
            targetChair = null;
            searchCooldown = SEARCH_COOLDOWN;
            stuckTicks = 0;
            lastPosition = null;
            entity.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (targetChair == null) {
                return;
            }

            double distanceToChair = entity.distanceToSqr(targetChair.getX(), targetChair.getY(), targetChair.getZ());

            // Check if we're close enough to sit
            if (distanceToChair <= CHAIR_REACH_DISTANCE * CHAIR_REACH_DISTANCE) {
                // Double-check the chair is still available
                if (isChairAvailable(targetChair)) {
                    sitInChair();
                }
                return;
            }

            // Check if we're stuck
            Vec3 currentPosition = entity.position();
            if (lastPosition != null) {
                double movementDistance = currentPosition.distanceTo(lastPosition);
                if (movementDistance < MOVEMENT_THRESHOLD) {
                    stuckTicks++;
                } else {
                    stuckTicks = 0; // Reset if we're moving
                }
            }
            lastPosition = currentPosition;

            // Try to navigate to the chair
            if (entity.getNavigation().isDone() || stuckTicks > 20) {
                // Recalculate path if navigation is done or we've been stuck for a bit
                entity.getNavigation().moveTo(targetChair.getX(), targetChair.getY(), targetChair.getZ(), 1.0);

                // If we're really stuck, try finding a different chair
                if (stuckTicks > 40) {
                    findNearbyEmptyChair();
                    if (targetChair != null) {
                        entity.getNavigation().moveTo(targetChair.getX(), targetChair.getY(), targetChair.getZ(), 1.0);
                    }
                }
            }
        }

        // Method to notify this goal that entity was manually unseated
        public void notifyManualUnseat() {
            wasManuallyUnseated = true;
            searchCooldown = MANUAL_UNSEAT_COOLDOWN; // Set longer cooldown
            targetChair = null;
            entity.getNavigation().stop();
        }

        private boolean findNearbyEmptyChair() {
            if (!(entity.level() instanceof ServerLevel serverLevel)) {
                return false;
            }

            AABB searchArea = new AABB(entity.blockPosition()).inflate(SEARCH_RADIUS);
            BlockPos entityPos = entity.blockPosition();
            BlockPos closestChair = null;
            double closestDistance = Double.MAX_VALUE;

            // Search for chair blocks in the area
            for (int x = (int) searchArea.minX; x <= searchArea.maxX; x++) {
                for (int y = (int) searchArea.minY; y <= searchArea.maxY; y++) {
                    for (int z = (int) searchArea.minZ; z <= searchArea.maxZ; z++) {
                        BlockPos checkPos = new BlockPos(x, y, z);

                        if (isChairAvailable(checkPos)) {
                            double distance = entityPos.distSqr(checkPos);
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                closestChair = checkPos;
                            }
                        }
                    }
                }
            }

            if (closestChair != null) {
                targetChair = closestChair;
                return true;
            }

            return false;
        }

        private boolean isChairAvailable(BlockPos chairPos) {
            if (!(entity.level() instanceof ServerLevel serverLevel)) {
                return false;
            }

            // Check if it's actually a chair block
            BlockState blockState = serverLevel.getBlockState(chairPos);
            if (!(blockState.getBlock() instanceof ChairBlock)) {
                return false;
            }

            // Check if chair is already occupied
            AbstractMercenaryEntity occupant = ChairBlock.getSittingEntity(serverLevel, chairPos);
            return occupant == null;
        }

        private void sitInChair() {
            if (targetChair == null || !(entity.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            // Final check that chair is available
            if (!isChairAvailable(targetChair)) {
                return;
            }

            // Position the entity properly and make them sit
            BlockState chairState = serverLevel.getBlockState(targetChair);
            Vec3 sittingPos = ChairBlock.getSittingPosition(targetChair, chairState);
            float sittingYaw = ChairBlock.getSittingYaw(chairState);

            entity.moveTo(sittingPos.x, sittingPos.y, sittingPos.z, sittingYaw, 0);
            entity.sitInChair(targetChair);
        }
    }

    public static class SafeGenericOwnerHurtByTargetGoal extends GenericOwnerHurtByTargetGoal {
        private final AbstractMercenaryEntity mercenary;

        public SafeGenericOwnerHurtByTargetGoal(AbstractMercenaryEntity mercenary, Supplier<Entity> ownerSupplier) {
            super(mercenary, ownerSupplier);
            this.mercenary = mercenary;
        }

        @Override
        public boolean canUse() {
            // Don't target if no summoner
            if (mercenary.getSummoner() == null) {
                return false;
            }

            // Don't target if stunned or sitting
            if (mercenary.isStunned() || mercenary.isSittingInChair()) {
                return false;
            }

            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            // Stop if summoner becomes null
            if (mercenary.getSummoner() == null) {
                return false;
            }

            return super.canContinueToUse();
        }
    }

    public static class SafeGenericOwnerHurtTargetGoal extends GenericOwnerHurtTargetGoal {
        private final AbstractMercenaryEntity mercenary;

        public SafeGenericOwnerHurtTargetGoal(AbstractMercenaryEntity mercenary, Supplier<Entity> ownerSupplier) {
            super(mercenary, ownerSupplier);
            this.mercenary = mercenary;
        }

        @Override
        public boolean canUse() {
            // Don't target if no summoner
            if (mercenary.getSummoner() == null) {
                return false;
            }

            // Don't target if stunned or sitting
            if (mercenary.isStunned() || mercenary.isSittingInChair()) {
                return false;
            }

            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            // Stop if summoner becomes null
            if (mercenary.getSummoner() == null) {
                return false;
            }

            return super.canContinueToUse();
        }
    }

    public static class SafeGenericCopyOwnerTargetGoal extends GenericCopyOwnerTargetGoal {
        private final AbstractMercenaryEntity mercenary;

        public SafeGenericCopyOwnerTargetGoal(AbstractMercenaryEntity mercenary, Supplier<Entity> ownerSupplier) {
            super(mercenary, ownerSupplier);
            this.mercenary = mercenary;
        }

        @Override
        public boolean canUse() {
            // Don't target if no summoner
            if (mercenary.getSummoner() == null) {
                return false;
            }

            // Don't target if stunned or sitting
            if (mercenary.isStunned() || mercenary.isSittingInChair()) {
                return false;
            }

            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            // Stop if summoner becomes null
            if (mercenary.getSummoner() == null) {
                return false;
            }

            return super.canContinueToUse();
        }
    }

    public static class SafeGenericProtectOwnerTargetGoal extends GenericProtectOwnerTargetGoal {
        private final AbstractMercenaryEntity mercenary;

        public SafeGenericProtectOwnerTargetGoal(AbstractMercenaryEntity mercenary, Supplier<Entity> ownerSupplier) {
            super(mercenary, ownerSupplier);
            this.mercenary = mercenary;
        }

        @Override
        public boolean canUse() {
            // Don't target if no summoner
            if (mercenary.getSummoner() == null) {
                return false;
            }

            // Don't target if stunned or sitting
            if (mercenary.isStunned() || mercenary.isSittingInChair()) {
                return false;
            }

            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            // Stop if summoner becomes null
            if (mercenary.getSummoner() == null) {
                return false;
            }

            return super.canContinueToUse();
        }
    }
}