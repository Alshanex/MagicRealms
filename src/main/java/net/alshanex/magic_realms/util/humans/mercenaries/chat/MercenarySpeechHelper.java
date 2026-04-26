package net.alshanex.magic_realms.util.humans.mercenaries.chat;

import net.alshanex.magic_realms.data.PersonalityData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.IExclusiveMercenary;
import net.alshanex.magic_realms.entity.exclusive.aliana.AlianaEntity;
import net.alshanex.magic_realms.entity.exclusive.amadeus.AmadeusEntity;
import net.alshanex.magic_realms.entity.exclusive.lilac.LilacEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.Hobby;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Picks a random speech line for a contracted mercenary when their contractor right-clicks them.
 *
 * <p>The pool is built from:
 * <ul>
 *   <li>the mercenary's rolled hobby response pool (archetype-specific, falling back to default)</li>
 *   <li>any extra translation keys exposed by an exclusive mercenary via
 *       {@link IExclusiveMercenary#getExclusiveSpeechTranslationKeys()}</li>
 * </ul>
 *
 * <p>A small per-mercenary cooldown is applied so spam-clicking doesn't fill chat with repeated lines, but the
 * cooldown is short enough that picking up a conversation a few seconds later still works.
 */
public final class MercenarySpeechHelper {

    /** Minimum ticks between two speech lines from the same mercenary. */
    private static final long PER_MERCENARY_COOLDOWN_TICKS = 20L * 4; // 4 seconds

    private static final Map<UUID, Long> lastSpeechTick = new HashMap<>();

    private MercenarySpeechHelper() {}

    /**
     * Try to send a random speech line from the mercenary to the player. Returns true if a line was actually sent
     * (and the cooldown should be considered consumed by the caller's perspective). Returns false if the cooldown is
     * still active, the merc has no lines, or the mercenary's personality isn't initialized.
     */
    public static boolean trySpeak(AbstractMercenaryEntity mercenary, ServerPlayer player) {
        if (mercenary == null || player == null) return false;

        long now = mercenary.level().getGameTime();
        Long lastTick = lastSpeechTick.get(mercenary.getUUID());
        if (lastTick != null && (now - lastTick) < PER_MERCENARY_COOLDOWN_TICKS) {
            return false;
        }

        List<String> pool = buildSpeechPool(mercenary);
        if (pool.isEmpty()) return false;

        String translationKey = pool.get(mercenary.getRandom().nextInt(pool.size()));
        MutableComponent component = MercenaryMessageFormatter.buildFor(mercenary, translationKey);
        player.sendSystemMessage(component);

        lastSpeechTick.put(mercenary.getUUID(), now);

        // Periodically prune stale entries so the map doesn't grow unbounded.
        if ((now % 1200L) == 0) {
            long staleCutoff = now - (20L * 60 * 5);
            lastSpeechTick.entrySet().removeIf(e -> e.getValue() < staleCutoff);
        }

        return true;
    }

    /**
     * Assemble all available translation keys for this mercenary's right-click speech.
     */
    private static List<String> buildSpeechPool(AbstractMercenaryEntity mercenary) {
        List<String> result = new ArrayList<>();

        PersonalityData personality = mercenary.getData(MRDataAttachments.PERSONALITY);
        if (personality != null && personality.isInitialized()) {
            Hobby hobby = personality.getHobby(false);
            if (hobby != null) {
                String archetypeId = personality.getArchetypeId();
                List<String> hobbyPool = hobby.getResponsePool(archetypeId);
                if (hobbyPool != null && !hobbyPool.isEmpty()) {
                    result.addAll(hobbyPool);
                }
            }
        }

        if (mercenary instanceof IExclusiveMercenary exclusive) {
            List<String> extra = exclusive.getExclusiveSpeechTranslationKeys();
            if (extra != null && !extra.isEmpty()) {
                if(mercenary instanceof LilacEntity lilac && !lilac.level().isClientSide() && lilac.getSummoner() != null){
                    if(hasContractedAlianaNearby(lilac, lilac.level())){
                        result.addAll(extra);
                    }
                } else {
                    result.addAll(extra);
                }
            }
        }

        return result;
    }

    private static boolean hasContractedAlianaNearby(LilacEntity entity, Level level) {
        double SEARCH_RADIUS = 20.0;
        AABB searchArea = new AABB(
                entity.getX() - SEARCH_RADIUS,
                entity.getY() - SEARCH_RADIUS,
                entity.getZ() - SEARCH_RADIUS,
                entity.getX() + SEARCH_RADIUS,
                entity.getY() + SEARCH_RADIUS,
                entity.getZ() + SEARCH_RADIUS
        );

        List<AlianaEntity> nearbyAliana = level.getEntitiesOfClass(
                AlianaEntity.class,
                searchArea,
                alianaEntity -> alianaEntity.getSummoner() != null && alianaEntity.getSummoner().is(entity.getSummoner())
        );

        return !nearbyAliana.isEmpty();
    }
}
