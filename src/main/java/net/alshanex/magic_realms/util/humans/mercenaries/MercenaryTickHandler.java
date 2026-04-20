package net.alshanex.magic_realms.util.humans.mercenaries;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.alshanex.magic_realms.Config;
import net.alshanex.magic_realms.block.ChairBlock;
import net.alshanex.magic_realms.data.ChairSittingData;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.events.MagicAttributeGainsHandler;
import net.alshanex.magic_realms.particles.StunParticleEffect;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * <p>Handles: chair sitting, allied-target cleanup, chair conflicts, stun particles, goal re-initialization after load, natural regeneration, stun processing, contract
 * tick-down, archer charge-animation tracking, and magic school XP gain on cast end.
 */
public final class MercenaryTickHandler {

    /** Chair rules: minimum sit time before a random unseat can fire. */
    private static final int MIN_SITTING_TIME = 200;   // 10 s

    /** Chair rules: after this many ticks, unseat chance jumps up. */
    private static final int MAX_SITTING_TIME = 1200;  // 60 s

    /** Natural regeneration interval when the kill-tracker flag is set. */
    private static final int REGEN_INTERVAL = 100;     // 5 s

    private MercenaryTickHandler() {}

    /** Main entry point from {@link AbstractMercenaryEntity#tick()}. */
    public static void tick(AbstractMercenaryEntity entity) {
        boolean serverSide = !entity.level().isClientSide;

        if (serverSide) {
            handleSittingTick(entity);
        }

        if (serverSide && entity.tickCount % 20 == 0 && entity.getTarget() != null) {
            if (entity.isAlliedTo(entity.getTarget())) {
                entity.setTarget(null);
            }
        }

        if (serverSide && entity.tickCount % 20 == 0 && entity.isSittingInChair()) {
            resolveChairConflict(entity);
        }

        if (!serverSide && entity.isStunned()) {
            StunParticleEffect.spawnStunParticles(entity, entity.level());
        }

        if (serverSide && !entity.areGoalsInitialized() && entity.isInitialized()) {
            entity.reinitializeGoalsAfterLoad();
            entity.setGoalsInitialized(true);
        }

        if (serverSide) {
            KillTrackerData killData = entity.getData(MRDataAttachments.KILL_TRACKER);
            if (killData.hasNaturalRegen() && entity.getHealth() < entity.getMaxHealth()) {
                handleNaturalRegeneration(entity);
            }
            if (entity.isStunned()) {
                handleStunTick(entity);
            }
        }

        if (serverSide && entity.tickCount % 20 == 0) {
            handleContractTick(entity);
        }

        if (entity.isArcher()) {
            entity.getArchery().tickAnimations();
            if (serverSide && entity.getTarget() != null && entity.tickCount % 10 == 0) {
                if (!entity.getArchery().hasArrows()) {
                    entity.setTarget(null);
                }
            }
        }

        if (serverSide) {
            handleCastTracking(entity);
        }
    }

    // ==================================================================
    // Sub-handlers
    // ==================================================================

    private static void handleSittingTick(AbstractMercenaryEntity entity) {
        ChairSittingData chair = entity.getChairData();
        chair.tickCooldown();

        if (!entity.isSittingInChair()) return;

        chair.incrementSittingTime();

        BlockPos chairPos = entity.getChairPosition();
        if (!isValidChair(entity, chairPos)) {
            entity.unsitFromChair();
            return;
        }

        Vec3 sittingPos = ChairBlock.getSittingPosition(chairPos, entity.level().getBlockState(chairPos));
        if (entity.position().distanceTo(sittingPos) > 0.5) {
            entity.moveTo(sittingPos.x, sittingPos.y, sittingPos.z, entity.getYRot(), entity.getXRot());
        }

        int sittingTime = chair.getSittingTime();
        if (sittingTime >= MIN_SITTING_TIME) {
            float unsitChance = sittingTime > MAX_SITTING_TIME ? 0.1f : 0.01f;
            if (entity.getRandom().nextFloat() < unsitChance) {
                entity.unsitFromChair();
            }
        }
    }

    private static boolean isValidChair(AbstractMercenaryEntity entity, BlockPos chairPos) {
        if (chairPos == null || chairPos.equals(BlockPos.ZERO)) return false;
        return entity.level().getBlockState(chairPos).getBlock() instanceof ChairBlock;
    }

    /**
     * If two mercenaries ended up on the same chair (rare race condition),
     * evicts everyone except the first one found.
     */
    private static void resolveChairConflict(AbstractMercenaryEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        BlockPos chairPos = entity.getChairPosition();
        if (chairPos == null || chairPos.equals(BlockPos.ZERO)) return;

        AABB searchArea = new AABB(chairPos).inflate(1.0);
        List<AbstractMercenaryEntity> sittingEntities = serverLevel.getEntitiesOfClass(
                AbstractMercenaryEntity.class,
                searchArea,
                e -> e.isSittingInChair() && chairPos.equals(e.getChairPosition())
        );

        if (sittingEntities.size() > 1) {
            for (int i = 1; i < sittingEntities.size(); i++) {
                sittingEntities.get(i).unsitFromChair();
            }
        }
    }

    private static void handleNaturalRegeneration(AbstractMercenaryEntity entity) {
        entity.regenTimer++;
        if (entity.regenTimer >= REGEN_INTERVAL) {
            float regenAmount = switch (entity.getStarLevel()) {
                case 1 -> 0.5f;  // 0.25 hearts
                case 2 -> 1.0f;  // 0.5 hearts
                case 3 -> 1.5f;  // 0.75 hearts
                default -> 1.0f;
            };
            entity.heal(regenAmount);
            entity.regenTimer = 0;
        }
    }

    private static void handleStunTick(AbstractMercenaryEntity entity) {
        if (!entity.isStunned()) return;
        if (entity.getStunTimer() <= 0) return;

        // Stunned mercs are locked down: no target, no movement, no casting.
        entity.setTarget(null);
        entity.getNavigation().stop();
        entity.setDeltaMovement(0, entity.getDeltaMovement().y * 0.1, 0);

        MagicData data = entity.getMagicData();
        if (data.isCasting()) {
            entity.cancelCast();
        }

        entity.decrementStunTimer();
        if (entity.getStunTimer() <= 0) {
            entity.setStunned(false);
            // Kick any eligible goals that were blocked during the stun.
            for (WrappedGoal wrappedGoal : entity.goalSelector.getAvailableGoals()) {
                Goal goal = wrappedGoal.getGoal();
                if (!wrappedGoal.isRunning() && wrappedGoal.canUse()) {
                    goal.start();
                }
            }
            // Come out of stun with a partial heal.
            entity.heal(entity.getMaxHealth() * 0.5f);
        }
    }

    private static void handleContractTick(AbstractMercenaryEntity entity) {
        ContractData contractData = entity.getData(MRDataAttachments.CONTRACT_DATA);

        if (contractData.hasActiveContract(entity.level())) {
            if (!contractData.isPermanent()) {
                if (entity.tickCount % 200 == 0) {
                    contractData.periodicTimeUpdate(entity.level());
                }
            }
            return;
        }

        // Contract just expired — notify the contractor.
        UUID previousContractorUUID = contractData.getContractorUUID();
        if (previousContractorUUID != null && !contractData.isPermanent()) {
            entity.clearContract();

            Player contractor = entity.level().getPlayerByUUID(previousContractorUUID);
            if (contractor instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "ui.magic_realms.contract_expired",
                        entity.getEntityName()).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /**
     * Detects when a spell cast just ended and fires the school-specific XP gain event. We track {@code wasCasting} and {@code lastCastingSpell}
     * across ticks so we can see the transition.
     */
    private static void handleCastTracking(AbstractMercenaryEntity entity) {
        MagicData data = entity.getMagicData();
        boolean isCasting = data.isCasting();

        if (entity.wasCasting && !isCasting && entity.lastCastingSpell != null) {
            MagicAttributeGainsHandler.handleSpellCast(entity, entity.lastCastingSpell.getSchoolType());
            entity.lastCastingSpell = null;
        }

        if (isCasting && data.getCastingSpell() != null) {
            entity.lastCastingSpell = data.getCastingSpell().getSpell();
        }

        entity.wasCasting = isCasting;
    }
}
