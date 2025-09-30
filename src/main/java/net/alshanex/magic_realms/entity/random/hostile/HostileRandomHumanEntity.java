package net.alshanex.magic_realms.entity.random.hostile;

import io.redspace.ironsspellbooks.entity.mobs.goals.WizardRecoverGoal;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.alshanex.magic_realms.item.PermanentContractItem;
import net.alshanex.magic_realms.item.TieredContractItem;
import net.alshanex.magic_realms.registry.MREntityRegistry;
import net.alshanex.magic_realms.registry.MRItems;
import net.alshanex.magic_realms.util.contracts.ContractUtils;
import net.alshanex.magic_realms.util.humans.goals.HumanGoals;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class HostileRandomHumanEntity extends RandomHumanEntity {
    private int waveSummoned = 1;

    public HostileRandomHumanEntity(EntityType<? extends RandomHumanEntity> entityType, Level level) {
        super(entityType, level);
    }

    public HostileRandomHumanEntity(Level level, int wave) {
        this(MREntityRegistry.HOSTILE_HUMAN.get(), level);
        this.waveSummoned = wave;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WizardRecoverGoal(this));
        this.goalSelector.addGoal(4, new HumanGoals.PickupMobDropsGoal(this));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true, false));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.FAIL;
        if (player.level().isClientSide()) return InteractionResult.FAIL;

        ItemStack heldItem = player.getItemInHand(hand);

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.FAIL;
        }

        if (heldItem.is(Items.EMERALD)) {
            return InteractionResult.FAIL;
        }

        if (heldItem.is(MRItems.HELL_PASS.get())) {
            return InteractionResult.FAIL;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected void handleContractInteraction(Player player, ContractData contractData, ItemStack heldItem) {

    }

    @Override
    protected void initializeHumanLevel(RandomSource randomSource, KillTrackerData killTrackerData) {
        killTrackerData.initializeRandomSpawnLevelForWave(randomSource, getWaveSummoned());
    }

    @Override
    protected boolean isHostileHuman() {
        return true;
    }

    private int getWaveSummoned(){
        return waveSummoned;
    }

    @Override
    public LivingEntity getSummoner() {
        return null;
    }

    @Override
    public void setSummoner(@Nullable LivingEntity owner) {

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
    }
}
