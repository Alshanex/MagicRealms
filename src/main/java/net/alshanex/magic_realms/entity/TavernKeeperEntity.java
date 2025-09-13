package net.alshanex.magic_realms.entity;

import com.google.common.collect.Sets;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardRecoverGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.IMerchantWizard;
import io.redspace.ironsspellbooks.player.AdditionalWanderingTrades;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.registry.MRItems;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;

import java.util.*;

public class TavernKeeperEntity extends NeutralWizard implements IAnimatedAttacker, IMerchantWizard {
    public TavernKeeperEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        xpReward = 0;
        this.getAttribute(AttributeRegistry.HOLY_SPELL_POWER).addPermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "tavernkeep"), 95.0, AttributeModifier.Operation.ADD_VALUE));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WizardAttackGoal(this, 1.25f, 25, 50)
                .setSpells(
                        List.of(SpellRegistry.SUNBEAM_SPELL.get()),
                        List.of(SpellRegistry.INVISIBILITY_SPELL.get()),
                        List.of(),
                        List.of(SpellRegistry.ROOT_SPELL.get(), SpellRegistry.INVISIBILITY_SPELL.get(), SpellRegistry.ABYSSAL_SHROUD_SPELL.get(), SpellRegistry.GREATER_HEAL_SPELL.get())
                )
                .setDrinksPotions()
                .setSingleUseSpell(SpellRegistry.SUMMON_SWORDS.get(), 1200, 2000, 4, 6)
        );
        this.goalSelector.addGoal(3, new PatrolNearLocationGoal(this, 5, .75f));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new WizardRecoverGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 1, true, false, this::isHostileTowards));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (level().isClientSide) {
            return false;
        }
        /*
        can parry:
        - serverside
        - in combat
        - we aren't in melee attack anim or spell cast
        - the damage source is caused by an entity (ie not fall damage)
        - the damage is caused within our rough field of vision (117 degrees)
        - the damage is not /kill
         */
        boolean canParry = this.isAggressive() &&
                !isImmobile() &&
                !isAnimating() &&
                pSource.getEntity() != null
                && !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (canParry) {
            serverTriggerAnimation("offhand_parry");
            this.playSound(SoundRegistry.ABYSSAL_TELEPORT.get());
            return false;
        }

        return super.hurt(pSource, pAmount);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean hasEffect(Holder<MobEffect> effect) {
        if(effect.is(MobEffectRegistry.ABYSSAL_SHROUD)){
            return true;
        }
        return super.hasEffect(effect);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance pPotioneffect) {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity pEntity) {
        return super.isAlliedTo(pEntity) || this.isAlliedHelper(pEntity);
    }

    private boolean isAlliedHelper(Entity entity) {
        return entity instanceof RandomHumanEntity human && human.getSummoner() == null;
    }

    @Override
    public boolean isHostileTowards(LivingEntity entity) {
        LivingEntity last = entity.getLastHurtMob();
        if (last != null) {
            if (this.isAlliedTo(last)) return true;
        }
        return super.isHostileTowards(entity);
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.MAX_HEALTH, 2000.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, .25);
    }

    @Override
    public Optional<SoundEvent> getAngerSound() {
        return Optional.of(SoundRegistry.TRADER_NO.get());
    }

    /**
     * Merchant implementations
     */

    @Nullable
    private Player tradingPlayer;
    @Nullable
    protected MerchantOffers offers;

    //Serialized
    private long lastRestockGameTime;
    private int numberOfRestocksToday;
    //Not Serialized
    private long lastRestockCheckDayTime;

    @Override
    protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        boolean preventTrade = (!this.level().isClientSide && this.getOffers().isEmpty()) || this.getTarget() != null || isAngryAt(pPlayer);
        if (pHand == InteractionHand.MAIN_HAND) {
            if (preventTrade && !this.level().isClientSide) {
                //this.setUnhappy();
            }
        }
        if (!preventTrade) {
            if (!this.level().isClientSide && !this.getOffers().isEmpty()) {
                if (shouldRestock()) {
                    restock();
                }
                this.startTrading(pPlayer);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(pPlayer, pHand);
    }

    private void startTrading(Player pPlayer) {
        this.setTradingPlayer(pPlayer);
        this.lookControl.setLookAt(pPlayer);
        this.openTradingScreen(pPlayer, this.getDisplayName(), 0);
    }

    @Override
    public int getRestocksToday() {
        return numberOfRestocksToday;
    }

    @Override
    public void setRestocksToday(int restocks) {
        this.numberOfRestocksToday = restocks;
    }

    @Override
    public long getLastRestockGameTime() {
        return lastRestockGameTime;
    }

    @Override
    public void setLastRestockGameTime(long time) {
        this.lastRestockGameTime = time;
    }

    @Override
    public long getLastRestockCheckDayTime() {
        return lastRestockCheckDayTime;
    }

    @Override
    public void setLastRestockCheckDayTime(long time) {
        this.lastRestockCheckDayTime = time;
    }

    @Override
    public void setTradingPlayer(@org.jetbrains.annotations.Nullable Player pTradingPlayer) {
        this.tradingPlayer = pTradingPlayer;
    }

    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = new MerchantOffers();

            this.offers.add(new AdditionalWanderingTrades.SimpleSell(3, new ItemStack(ItemRegistry.FIRE_ALE.get()), 3, 6).getOffer(this, this.random));
            this.offers.add(new AdditionalWanderingTrades.SimpleBuy(16, new ItemCost(MRItems.CONTRACT_NOVICE.get(), 1), 7, 10).getOffer(this, this.random));

            this.offers.addAll(createRandomOffers(1, 2));

            if (this.random.nextFloat() < 0.25f) {
                this.offers.add(
                        new MerchantOffer(
                                new ItemCost(ItemRegistry.BLOODY_VELLUM.get(), 1),
                                Optional.of(new ItemCost(MRItems.TIME_ESSENCE.get(), 1)),
                                new ItemStack(MRItems.CONTRACT_PERMANENT),
                                1,
                                0,
                                .05f
                        )
                );
            }
            this.offers.removeIf(Objects::isNull);
            //We count the creation of our stock as a restock so that we do not immediately refresh trades the same day.
            numberOfRestocksToday++;
        }
        return this.offers;
    }

    private static final List<VillagerTrades.ItemListing> fillerOffers = List.of(
            new AdditionalWanderingTrades.SimpleBuy(16, new ItemCost(MRItems.CONTRACT_APPRENTICE.get(), 1), 15, 20),
            new AdditionalWanderingTrades.SimpleBuy(16, new ItemCost(MRItems.CONTRACT_JOURNEYMAN.get(), 1), 25, 35),
            new AdditionalWanderingTrades.SimpleBuy(16, new ItemCost(MRItems.CONTRACT_EXPERT.get(), 1), 45, 55),
            new AdditionalWanderingTrades.SimpleBuy(16, new ItemCost(MRItems.CONTRACT_MASTER.get(), 1), 60, 64)
    );

    private Collection<MerchantOffer> createRandomOffers(int min, int max) {
        Set<Integer> set = Sets.newHashSet();
        int fillerTrades = random.nextIntBetweenInclusive(min, max);
        for (int i = 0; i < 10 && set.size() < fillerTrades; i++) {
            set.add(random.nextInt(fillerOffers.size()));
        }
        Collection<MerchantOffer> offers = new ArrayList<>();
        for (Integer integer : set) {
            offers.add(fillerOffers.get(integer).getOffer(this, this.random));
        }
        return offers;
    }

    @Override
    public void overrideOffers(MerchantOffers pOffers) {

    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isTrading();
    }

    @Override
    public void notifyTrade(MerchantOffer pOffer) {
        pOffer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        //this.rewardTradeXp(pOffer);
    }

    @Override
    public void notifyTradeUpdated(ItemStack pStack) {
        if (!this.level().isClientSide && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
            this.ambientSoundTime = -this.getAmbientSoundInterval();
            this.playSound(this.getTradeUpdatedSound(!pStack.isEmpty()), this.getSoundVolume(), this.getVoicePitch());
        }
    }

    protected SoundEvent getTradeUpdatedSound(boolean pIsYesSound) {
        return pIsYesSound ? SoundRegistry.TRADER_YES.get() : SoundRegistry.TRADER_NO.get();
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundRegistry.TRADER_YES.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        serializeMerchant(pCompound, this.offers, this.lastRestockGameTime, this.numberOfRestocksToday);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        deserializeMerchant(pCompound, c -> this.offers = c);
    }

    //Animations
    RawAnimation animationToPlay = null;
    private final AnimationController<TavernKeeperEntity> meleeController = new AnimationController<>(this, "keeper_animations", 0, this::predicate);

    @Override
    public void playAnimation(String animationId) {
        try {
            animationToPlay = RawAnimation.begin().thenPlay(animationId);
        } catch (Exception ignored) {
            MagicRealms.LOGGER.error("Entity {} Failed to play animation: {}", this, animationId);
        }
    }

    private PlayState predicate(AnimationState<TavernKeeperEntity> animationEvent) {
        var controller = animationEvent.getController();

        if (this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(meleeController);
        super.registerControllers(controllerRegistrar);
    }

    @Override
    public boolean isAnimating() {
        boolean meleeAnimating = meleeController.getAnimationState() != AnimationController.State.STOPPED;

        return meleeAnimating || super.isAnimating();
    }
}
