package net.alshanex.magic_realms.util.humans.mercenaries;

import net.alshanex.magic_realms.data.PersonalityData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.IExclusiveMercenary;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.Hobby;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.PersonalityArchetype;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Picks a random speech line for a contracted mercenary when their contractor right-clicks them.
 *
 * The pool is built from:
 *   - the mercenary's rolled hobby response pool (archetype-specific, falling back to default)
 *   - any extra translation keys exposed by an exclusive mercenary via {@link IExclusiveMercenary#getExclusiveSpeechTranslationKeys()}
 *
 * A small per-mercenary cooldown is applied so spam-clicking doesn't fill chat with repeated lines, but the cooldown is short
 * enough that picking up a conversation a few seconds later still works.
 */
public final class MercenarySpeechHelper {

    /** Minimum ticks between two speech lines from the same mercenary. */
    private static final long PER_MERCENARY_COOLDOWN_TICKS = 20L * 4; // 4 seconds

    private static final Map<UUID, Long> lastSpeechTick = new HashMap<>();

    private MercenarySpeechHelper() {}

    /**
     * Try to send a random speech line from the mercenary to the player. Returns true if a line was actually sent (and the cooldown should
     * be considered consumed by the caller's perspective). Returns false if the cooldown is still active, the merc has no lines, or the
     * mercenary's personality isn't initialized.
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
        MutableComponent component = Component.translatable(translationKey, mercenary.getEntityName())
                .withStyle(ChatFormatting.WHITE);
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
                PersonalityArchetype archetype = personality.getArchetype();
                String archetypeId = archetype != null ? archetype.getId() : null;
                List<String> hobbyPool = hobby.getResponsePool(archetypeId);
                if (hobbyPool != null && !hobbyPool.isEmpty()) {
                    result.addAll(hobbyPool);
                }
            }
        }

        if (mercenary instanceof IExclusiveMercenary exclusive) {
            List<String> extra = exclusive.getExclusiveSpeechTranslationKeys();
            if (extra != null && !extra.isEmpty()) {
                result.addAll(extra);
            }
        }

        return result;
    }
}
