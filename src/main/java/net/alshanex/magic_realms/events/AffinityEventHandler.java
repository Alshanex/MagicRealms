package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.AffinityOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wires gameplay events to AffinityOps so the personality state actually
 * responds to how the player treats their mercenaries.
 *
 * Events hooked here:
 *   - Player kills a merc's feared-entity type nearby        -> positive
 *   - Player and contracted merc kill a boss together        -> positive
 *   - Hell's Pass saves a merc from fatal damage             -> positive
 *   - Player heals a merc back from low HP                   -> positive
 */
@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AffinityEventHandler {

    /**
     * Per-merc last-known level. Polled every 10 ticks from the server tick handler. When we observe an increase for a contracted merc,
     * we credit the contractor once per level-up.
     */
    private static final Map<UUID, Integer> lastKnownLevel = new HashMap<>();

    /** Radius within which a contracted mercenary counts as "present" for shared-event credit. */
    private static final double PRESENCE_RADIUS = 24.0;

    /** Low-HP threshold for the heal-credit detector. */
    private static final float LOW_HP_FRACTION = 0.20f;

    /** Fraction the merc has to recover past to count as "healed back up". */
    private static final float HEAL_RECOVERY_FRACTION = 0.35f;

    /**
     * Per-merc timestamp of when they last entered low-HP. Used by the healing-credit detector to tell "just dropped to 10% then got
     * healed to 50%" apart from random unrelated heal events.
     */
    private static final Map<UUID, Long> wasLowRecently = new HashMap<>();
    private static final long LOW_HP_GRACE_TICKS = 20L * 10; // 10s window


    // =============== Healing credit (low-HP detector) ===============

    /**
     * Whenever a contracted merc takes damage that drops them below the low-HP threshold, stamp them. If they subsequently heal significantly
     * within the grace window, the healing-credit handler credits the nearby contractor.
     */
    @SubscribeEvent
    public static void onMercenaryLowHpMark(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractMercenaryEntity mercenary)) return;
        if (mercenary.level().isClientSide()) return;

        float pct = mercenary.getHealth() / Math.max(1.0f, mercenary.getMaxHealth());
        if (pct <= LOW_HP_FRACTION) {
            wasLowRecently.put(mercenary.getUUID(), mercenary.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onMercenaryHealed(LivingHealEvent event) {
        if (!(event.getEntity() instanceof AbstractMercenaryEntity mercenary)) return;
        if (mercenary.level().isClientSide()) return;
        if (event.getAmount() <= 0) return;

        Long lowStamp = wasLowRecently.get(mercenary.getUUID());
        if (lowStamp == null) return;

        long now = mercenary.level().getGameTime();
        if ((now - lowStamp) > LOW_HP_GRACE_TICKS) {
            wasLowRecently.remove(mercenary.getUUID());
            return;
        }

        // Check the heal actually carries them meaningfully above the low threshold.
        float pctAfter = (mercenary.getHealth() + event.getAmount()) / Math.max(1.0f, mercenary.getMaxHealth());
        if (pctAfter < HEAL_RECOVERY_FRACTION) return;

        // Credit the contractor if they're nearby. If not nearby, no credit - this is about "you helped me when I was dying", not regen ticks.
        ContractData contract = mercenary.getData(MRDataAttachments.CONTRACT_DATA);
        if (contract == null) return;
        UUID contractorId = contract.getContractorUUID();
        if (contractorId == null || !contract.isContractor(contractorId, mercenary.level())) return;

        Player contractor = findPlayer(mercenary.level(), contractorId);
        if (contractor == null) return;
        if (contractor.distanceToSqr(mercenary) > PRESENCE_RADIUS * PRESENCE_RADIUS) return;

        AffinityOps.onHealedWhenLow(mercenary, contractorId, now);
        wasLowRecently.remove(mercenary.getUUID());
    }

    // Kills

    /**
     * Player kills something:
     *   - If the victim matches the fear of any nearby contracted merc, that merc gets feared-foe credit.
     *   - If the victim is a boss and any nearby contracted merc is within presence radius, they get boss-together credit.
     */
    @SubscribeEvent
    public static void onLivingDeathForAffinity(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (victim.level().isClientSide()) return;
        if (!(victim.level() instanceof ServerLevel serverLevel)) return;

        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof Player player)) return;

        long now = serverLevel.getGameTime();

        List<AbstractMercenaryEntity> nearbyContracted = findContractedMercenariesNear(player, serverLevel);
        if (nearbyContracted.isEmpty()) return;

        EntityType<?> victimType = victim.getType();
        boolean victimIsBoss = victimType.is(ModTags.BOSSES_TAG);

        for (AbstractMercenaryEntity mercenary : nearbyContracted) {
            if (mercenary.isAfraidOf(victim)) {
                AffinityOps.onKilledFearedFoe(mercenary, player.getUUID());
            }
            if (victimIsBoss) {
                AffinityOps.onBossDefeatedTogether(mercenary, player.getUUID(), now);
            }
        }
    }

    // Hell's Pass save

    /**
     * Piggybacks on KillTrackingHandler's immortality save. That handler runs on LivingIncomingDamageEvent at the default priority, cancels
     * the damage, sets the merc to half HP, and stuns them. We run at LOWEST priority AFTER it and detect the save by observing that the
     * event was cancelled on an immortal merc.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onImmortalitySavedMercenary(LivingIncomingDamageEvent event) {
        if (!event.isCanceled()) return;
        if (!(event.getEntity() instanceof AbstractMercenaryEntity mercenary)) return;
        if (mercenary.level().isClientSide()) return;
        if (!mercenary.isImmortal()) return;
        if (!mercenary.isStunned()) return;

        ContractData contract = mercenary.getData(MRDataAttachments.CONTRACT_DATA);
        if (contract == null) return;
        UUID contractorId = contract.getContractorUUID();
        if (contractorId == null || !contract.isContractor(contractorId, mercenary.level())) return;

        // No proximity requirement - the pass belongs to the contractor regardless of where they are when it saves their merc's life.
        AffinityOps.onHellPassSavedMe(mercenary, contractorId);
    }

    // Cleanup ticks

    /**
     * Periodic maintenance. Prune stale low-HP stamps and throttles.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer() == null) return;
        long now = event.getServer().overworld().getGameTime();

        if ((now % 1200L) == 0) {
            wasLowRecently.entrySet().removeIf(e -> (now - e.getValue()) > LOW_HP_GRACE_TICKS);
            AffinityOps.pruneThrottlesOlderThan(20L * 60 * 10, now); // 10 min
        }
    }

    // =============== Level-up detection ===============

    /**
     * Detect level-ups by observing LivingDeathEvent at LOWEST priority - this runs AFTER KillTrackingHandler's main handler which updates the
     * mercenary's killData.currentLevel. We compare the current value against our cached "last seen" value; if it's higher, it's a level-up.
     *
     * Handles the multi-level-in-one-kill case (a big XP boss) by issuing one credit per level gained.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMercenaryKillForLevelUp(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof AbstractMercenaryEntity mercenary)) return;
        if (mercenary.level().isClientSide()) return;

        int currentLevel = mercenary.getData(MRDataAttachments.KILL_TRACKER).getCurrentLevel();
        Integer prev = lastKnownLevel.put(mercenary.getUUID(), currentLevel);

        if (prev == null || currentLevel <= prev) return;

        ContractData contract = mercenary.getData(MRDataAttachments.CONTRACT_DATA);
        if (contract == null) return;
        UUID contractorId = contract.getContractorUUID();
        if (contractorId == null || !contract.isContractor(contractorId, mercenary.level())) return;

        int levelsGained = currentLevel - prev;
        for (int i = 0; i < levelsGained; i++) {
            AffinityOps.onLeveledUpUnderContract(mercenary, contractorId);
        }
    }

    /**
     * Seed the level cache when a mercenary first joins the level so we have a baseline to compare against on their next kill. Without this, the
     * FIRST level-up after a world load would be missed.
     */
    @SubscribeEvent
    public static void onMercenaryJoinLevel(
            net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractMercenaryEntity mercenary)) return;

        int currentLevel = mercenary.getData(MRDataAttachments.KILL_TRACKER).getCurrentLevel();
        lastKnownLevel.put(mercenary.getUUID(), currentLevel);
    }

    // Helpers

    private static List<AbstractMercenaryEntity> findContractedMercenariesNear(Player player, ServerLevel level) {
        AABB box = new AABB(
                player.getX() - PRESENCE_RADIUS, player.getY() - PRESENCE_RADIUS, player.getZ() - PRESENCE_RADIUS,
                player.getX() + PRESENCE_RADIUS, player.getY() + PRESENCE_RADIUS, player.getZ() + PRESENCE_RADIUS
        );
        UUID playerId = player.getUUID();
        return level.getEntitiesOfClass(AbstractMercenaryEntity.class, box, merc -> {
            if (merc.isRemoved() || !merc.isAlive()) return false;
            ContractData c = merc.getData(MRDataAttachments.CONTRACT_DATA);
            return c != null && c.isContractor(playerId, level);
        });
    }

    private static Player findPlayer(Level level, UUID uuid) {
        if (level instanceof ServerLevel serverLevel) {
            Entity e = serverLevel.getEntity(uuid);
            return e instanceof Player p ? p : null;
        }
        return null;
    }
}
