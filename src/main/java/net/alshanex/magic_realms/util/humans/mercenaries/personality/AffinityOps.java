package net.alshanex.magic_realms.util.humans.mercenaries.personality;

import net.alshanex.magic_realms.data.PersonalityData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central operations for adjusting a mercenary's affinity with a player, plus (usually) recording a MemoryEvent at the same time.
 */
public final class AffinityOps {

    private AffinityOps() {}

    // Positive events
    public static final int AFFINITY_CONTRACT_RENEWED = 2;
    public static final int AFFINITY_CONTRACT_PERMANENT = 15;
    public static final int AFFINITY_GIFT_FAVORITE = 10;
    public static final int AFFINITY_GIFT_NEUTRAL_FOOD = 1;
    public static final int AFFINITY_GIFT_HELL_PASS = 20;
    public static final int AFFINITY_KILLED_FEARED_FOE = 5;
    public static final int AFFINITY_BOSS_TOGETHER = 3;
    public static final int AFFINITY_LEVELED_UP = 1;
    public static final int AFFINITY_HELL_PASS_SAVED_ME = 15;
    public static final int AFFINITY_HEALED_WHEN_LOW = 5;

    // Negative events
    public static final int AFFINITY_GIFT_DISLIKED = -5;
    public static final int AFFINITY_FRIENDLY_FIRE = -8;

    /** Ticks between friendly-fire affinity hits from the same player. */
    public static final long FRIENDLY_FIRE_COOLDOWN_TICKS = 20L * 60;   // 60s

    /** Ticks between shared-boss-kill credits (so one boss = one bump, not spammed). */
    public static final long BOSS_TOGETHER_COOLDOWN_TICKS = 20L * 30;   // 30s

    /** Ticks between "healed when low" credits (prevents spam from stacking hits). */
    public static final long HEAL_WHEN_LOW_COOLDOWN_TICKS = 20L * 30;

    private record ThrottleKey(UUID merc, UUID player, String eventId) {}
    private static final Map<ThrottleKey, Long> throttleTimestamps = new HashMap<>();

    /**
     * Apply a delta and optionally record a memory event in one call. Callers never poke PersonalityData directly - they go through this.
     */
    public static void apply(AbstractMercenaryEntity mercenary, UUID playerId, int delta, @Nullable MemoryEvent memory) {
        if (mercenary == null || playerId == null) return;
        PersonalityData data = mercenary.getData(MRDataAttachments.PERSONALITY);
        if (data == null) return;

        if (delta != 0) data.adjustAffinity(playerId, delta);
        if (memory != null) data.addMemory(playerId, memory);
    }

    public static void onContractRenewed(AbstractMercenaryEntity mercenary, UUID playerId) {
        apply(mercenary, playerId, AFFINITY_CONTRACT_RENEWED, null);
    }

    public static void onContractPermanent(AbstractMercenaryEntity mercenary, UUID playerId) {
        apply(mercenary, playerId, AFFINITY_CONTRACT_PERMANENT, null);
    }

    public static void onGiftFavorite(AbstractMercenaryEntity mercenary, UUID playerId) {
        apply(mercenary, playerId, AFFINITY_GIFT_FAVORITE, MemoryEvent.GIFTED_FAVORITE_FOOD);
    }

    public static void onGiftDisliked(AbstractMercenaryEntity mercenary, UUID playerId) {
        apply(mercenary, playerId, AFFINITY_GIFT_DISLIKED, MemoryEvent.GIFTED_DISLIKED_FOOD);
    }

    public static void onGiftNeutralFood(AbstractMercenaryEntity mercenary, UUID playerId) {
        apply(mercenary, playerId, AFFINITY_GIFT_NEUTRAL_FOOD, null);
    }

    public static void onGiftHellPass(AbstractMercenaryEntity mercenary, UUID playerId) {
        apply(mercenary, playerId, AFFINITY_GIFT_HELL_PASS, null);
    }

    public static void onKilledFearedFoe(AbstractMercenaryEntity mercenary, UUID playerId) {
        apply(mercenary, playerId, AFFINITY_KILLED_FEARED_FOE, MemoryEvent.KILLED_FEARED_FOE);
    }

    public static void onLeveledUpUnderContract(AbstractMercenaryEntity mercenary, UUID playerId) {
        apply(mercenary, playerId, AFFINITY_LEVELED_UP, MemoryEvent.LEVELED_UP_UNDER_CONTRACT);
    }

    public static void onHellPassSavedMe(AbstractMercenaryEntity mercenary, UUID playerId) {
        apply(mercenary, playerId, AFFINITY_HELL_PASS_SAVED_ME, MemoryEvent.NEAR_DEATH_SURVIVED);
    }

    /**
     * Boss-kill credit, throttled per (merc, player) pair so a boss that takes multiple LivingDeathEvents doesn't double-credit.
     */
    public static void onBossDefeatedTogether(AbstractMercenaryEntity mercenary, UUID playerId, long currentTick) {
        if (!throttleGate(mercenary.getUUID(), playerId, "boss", currentTick, BOSS_TOGETHER_COOLDOWN_TICKS)) return;
        apply(mercenary, playerId, AFFINITY_BOSS_TOGETHER, MemoryEvent.BOSS_DEFEATED_TOGETHER);
    }

    /**
     * Friendly-fire affinity hit, throttled per player per merc per minute - accidents shouldn't compound across every arrow in a battle.
     */
    public static void onFriendlyFire(AbstractMercenaryEntity mercenary, UUID playerId, long currentTick) {
        if (!throttleGate(mercenary.getUUID(), playerId, "ff", currentTick, FRIENDLY_FIRE_COOLDOWN_TICKS)) return;
        apply(mercenary, playerId, AFFINITY_FRIENDLY_FIRE, MemoryEvent.FRIENDLY_FIRE);
    }

    /**
     * Credit a player for healing a mercenary at low HP, throttled so a burst of heals over a few seconds counts as one "saved me" moment.
     */
    public static void onHealedWhenLow(AbstractMercenaryEntity mercenary, UUID playerId, long currentTick) {
        if (!throttleGate(mercenary.getUUID(), playerId, "heal_low", currentTick, HEAL_WHEN_LOW_COOLDOWN_TICKS)) return;
        apply(mercenary, playerId, AFFINITY_HEALED_WHEN_LOW, MemoryEvent.SAVED_FROM_DEATH);
    }


    private static boolean throttleGate(UUID mercUuid, UUID playerUuid, String eventId, long now, long cooldownTicks) {
        ThrottleKey key = new ThrottleKey(mercUuid, playerUuid, eventId);
        Long last = throttleTimestamps.get(key);
        if (last != null && (now - last) < cooldownTicks) return false;
        throttleTimestamps.put(key, now);
        return true;
    }

    public static void pruneThrottlesOlderThan(long cutoffTicks, long now) {
        throttleTimestamps.entrySet().removeIf(e -> (now - e.getValue()) > cutoffTicks);
    }


    // Food-gift dispatch

    /**
     * Process a player handing a food item to a contracted mercenary.
     * Classifies the stack against the merc's rolled food-preference tags and applies the corresponding affinity change + memory event.
     *
     * Returns true if the stack was actually consumed as a gift.
     * Returns false if the item isn't recognized as food at all.
     */
    public static boolean tryGiftFood(AbstractMercenaryEntity mercenary, net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        // Use the data-component food check; any stack with a FOOD component counts as food for gift purposes.
        if (stack.get(DataComponents.FOOD) == null) return false;

        PersonalityData data = mercenary.getData(MRDataAttachments.PERSONALITY);
        if (data == null || !data.isInitialized()) return false;

        UUID playerId = player.getUUID();
        TagKey<Item> fav = data.getFavoriteFoodTag();
        TagKey<net.minecraft.world.item.Item> dis = data.getDislikedFoodTag();

        if (fav != null && stack.is(fav)) {
            onGiftFavorite(mercenary, playerId);
            return true;
        }
        if (dis != null && stack.is(dis)) {
            onGiftDisliked(mercenary, playerId);
            return true;
        }
        onGiftNeutralFood(mercenary, playerId);
        return true;
    }
}
