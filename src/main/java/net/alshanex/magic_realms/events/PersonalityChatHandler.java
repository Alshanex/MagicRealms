package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.data.PersonalityData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.Hobby;
import net.alshanex.magic_realms.util.humans.mercenaries.personality.PersonalityArchetype;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Generalized keyword chat for contracted mercenaries. When a player types in chat, any nearby mercenary they have contracted may respond if the
 * message contains one of the keywords associated with that mercenary's rolled hobby.
 *
 * Design constraints:
 *   - ONLY contracted mercenaries respond, and only to their own contractor (so random tavern mercenaries don't spam chat)
 *   - Per-mercenary cooldown prevents the same merc chain-responding to every message you type
 *   - Per-player cooldown prevents a stray word from triggering a group chorus from every mercenary in range
 *   - Combat-locked mercenaries stay silent
 *   - Responses use a small random delay (10-40 ticks) for natural feel and staggering when multiple mercs respond
 */
@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class PersonalityChatHandler {

    /** Radius within which a contracted mercenary can "hear" the player's chat. */
    private static final double HEARING_RADIUS = 12.0;

    /** Minimum ticks between hobby responses from the SAME mercenary. */
    private static final long PER_MERCENARY_COOLDOWN_TICKS = 20L * 45;   // 45s

    /** Minimum ticks between hobby responses to the SAME player (across all mercenaries). */
    private static final long PER_PLAYER_COOLDOWN_TICKS = 20L * 8;       // 8s

    /** Max number of mercenaries that can respond to a single message. */
    private static final int MAX_RESPONDERS_PER_MESSAGE = 2;

    /** Random delay range for the response, in ticks. */
    private static final int MIN_RESPONSE_DELAY_TICKS = 10;
    private static final int MAX_RESPONSE_DELAY_TICKS = 40;

    /** Extra spacing when multiple mercs respond - avoids simultaneous chat lines. */
    private static final int STAGGER_SPACING_TICKS = 15;

    /** Last-response game time per mercenary UUID. */
    private static final Map<UUID, Long> lastMercenaryResponseTick = new HashMap<>();

    /** Last-response game time per player UUID. */
    private static final Map<UUID, Long> lastPlayerReceivedTick = new HashMap<>();

    /**
     * Scheduled messages queued to fire on a future server tick.
     */
    private static final Map<Long, List<Runnable>> scheduledResponses = new HashMap<>();

    /** Dedup for hobby keyword regex patterns */
    private static final Map<String, Pattern> keywordPatternCache = new HashMap<>();

    // Chat event entry point

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide()) return;

        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        long now = serverLevel.getGameTime();

        // Per-player global cooldown - if we just responded to this player within PER_PLAYER_COOLDOWN_TICKS, skip entirely.
        Long lastPlayerTick = lastPlayerReceivedTick.get(player.getUUID());
        if (lastPlayerTick != null && (now - lastPlayerTick) < PER_PLAYER_COOLDOWN_TICKS) {
            return;
        }

        String lowerMessage = event.getMessage().getString().toLowerCase();

        List<AbstractMercenaryEntity> nearbyContracted = findContractedMercenariesNear(player, serverLevel);
        if (nearbyContracted.isEmpty()) return;

        int responders = 0;
        for (AbstractMercenaryEntity mercenary : nearbyContracted) {
            if (responders >= MAX_RESPONDERS_PER_MESSAGE) break;

            if (!canMercenaryRespond(mercenary, now)) continue;

            // Resolve hobby on the server side.
            PersonalityData personality = mercenary.getData(MRDataAttachments.PERSONALITY);
            Hobby hobby = personality.getHobby(false);
            if (hobby == null || hobby.keywords().isEmpty()) continue;

            if (!messageMatchesHobby(lowerMessage, hobby)) continue;

            // Pick a response translation key appropriate for this mercenary's archetype.
            String translationKey = pickResponseKey(hobby, personality.getArchetype(), mercenary);
            if (translationKey == null) continue;

            scheduleHobbyResponse(player, mercenary, translationKey, now, responders);
            lastMercenaryResponseTick.put(mercenary.getUUID(), now);
            lastPlayerReceivedTick.put(player.getUUID(), now);
            responders++;
        }
    }

    // Helpers

    /**
     * Find mercenaries in HEARING_RADIUS of the player that are contracted by THIS player. Filters out unavailable mercenaries (stunned, removed).
     * Sitting is allowed - a merc resting in the tavern should still hear you. Combat-locked is rejected so responses don't stack on top of combat chat.
     */
    private static List<AbstractMercenaryEntity> findContractedMercenariesNear(Player player, ServerLevel level) {
        AABB searchArea = new AABB(
                player.getX() - HEARING_RADIUS, player.getY() - HEARING_RADIUS, player.getZ() - HEARING_RADIUS,
                player.getX() + HEARING_RADIUS, player.getY() + HEARING_RADIUS, player.getZ() + HEARING_RADIUS
        );

        UUID playerId = player.getUUID();
        return level.getEntitiesOfClass(AbstractMercenaryEntity.class, searchArea, mercenary -> {
            if (mercenary.isRemoved() || !mercenary.isAlive()) return false;
            ContractData contract = mercenary.getData(MRDataAttachments.CONTRACT_DATA);
            return contract != null && contract.isContractor(playerId, level);
        });
    }

    /**
     * Per-mercenary gating. Bundles all the "is this merc even in a state to talk?" checks in one place.
     */
    private static boolean canMercenaryRespond(AbstractMercenaryEntity mercenary, long now) {
        if (mercenary.isStunned()) return false;
        if (mercenary.isInCombat()) return false;

        Long lastTick = lastMercenaryResponseTick.get(mercenary.getUUID());
        if (lastTick != null && (now - lastTick) < PER_MERCENARY_COOLDOWN_TICKS) return false;

        PersonalityData personality = mercenary.getData(MRDataAttachments.PERSONALITY);
        return personality.isInitialized();
    }

    /**
     * Test if the player's (already lowercased) message matches ANY of the hobby's keywords on a word boundary. Patterns are cached to avoid
     * recompiling per-chat-event.
     */
    private static boolean messageMatchesHobby(String lowerMessage, Hobby hobby) {
        for (String keyword : hobby.keywords()) {
            if (keyword == null || keyword.isEmpty()) continue;
            Pattern pattern = keywordPatternCache.computeIfAbsent(
                    keyword.toLowerCase(),
                    kw -> Pattern.compile("\\b" + Pattern.quote(kw) + "\\b", Pattern.CASE_INSENSITIVE)
            );
            if (pattern.matcher(lowerMessage).find()) return true;
        }
        return false;
    }

    /**
     * Choose a response translation key for this mercenary. Prefer the archetype-specific pool; fall back to "default"; return null if
     * neither has entries. The random variant within the pool is picked using the mercenary's own RandomSource so the choice feels like
     * "what this particular merc would say".
     */
    private static String pickResponseKey(Hobby hobby, PersonalityArchetype archetype, AbstractMercenaryEntity mercenary) {
        String archetypeId = archetype != null ? archetype.getId() : null;
        List<String> pool = hobby.getResponsePool(archetypeId);
        if (pool.isEmpty()) return null;
        return pool.get(mercenary.getRandom().nextInt(pool.size()));
    }

    // Scheduled dispatch

    /**
     * Schedule a response to fire on a future tick. Base delay is a random value between MIN and MAX; subsequent responders within the same
     * message get staggered further to avoid overlapping chat lines.
     */
    private static void scheduleHobbyResponse(Player player, AbstractMercenaryEntity mercenary,
                                              String translationKey, long now, int responderIndex) {
        int baseDelay = MIN_RESPONSE_DELAY_TICKS +
                mercenary.getRandom().nextInt(MAX_RESPONSE_DELAY_TICKS - MIN_RESPONSE_DELAY_TICKS + 1);
        int stagger = responderIndex * STAGGER_SPACING_TICKS;
        long executeTick = now + baseDelay + stagger;

        String mercName = mercenary.getEntityName();
        UUID mercUuid = mercenary.getUUID();
        UUID playerUuid = player.getUUID();

        scheduledResponses.computeIfAbsent(executeTick, k -> new ArrayList<>()).add(() -> {
            // Re-fetch at execution time: player may have logged out, mercenary may have moved out of range, contract may have expired.
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!serverPlayer.isAlive() || serverPlayer.isRemoved()) return;

            AbstractMercenaryEntity resolved = resolveMercenary(serverPlayer.serverLevel(), mercUuid);
            if (resolved == null || resolved.isRemoved()) return;
            if (resolved.distanceToSqr(serverPlayer) > HEARING_RADIUS * HEARING_RADIUS) return;

            ContractData contract = resolved.getData(MRDataAttachments.CONTRACT_DATA);
            if (contract == null || !contract.isContractor(playerUuid, serverPlayer.serverLevel())) return;

            MutableComponent component = Component.translatable(translationKey, mercName).withStyle(ChatFormatting.GOLD);
            serverPlayer.sendSystemMessage(component);
        });
    }

    /**
     * Locate a mercenary by UUID in the given server level. Returns null if it's unloaded or gone. We don't try to find it across dimensions -
     * if the player changed dimensions while a response was scheduled, the response is dropped.
     */
    private static AbstractMercenaryEntity resolveMercenary(ServerLevel level, UUID mercUuid) {
        var entity = level.getEntity(mercUuid);
        return entity instanceof AbstractMercenaryEntity merc ? merc : null;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;

        long now = server.overworld().getGameTime();

        // Fire anything due. We iterate snapshot keys to avoid ConcurrentModificationException - scheduled runnables could in theory enqueue new ones, so removing-as-we-go is safer.
        List<Long> dueKeys = new ArrayList<>();
        for (Long key : scheduledResponses.keySet()) {
            if (key <= now) dueKeys.add(key);
        }

        for (Long key : dueKeys) {
            List<Runnable> bucket = scheduledResponses.remove(key);
            if (bucket != null) {
                for (Runnable r : bucket) {
                    try {
                        r.run();
                    } catch (Exception e) {
                        MagicRealms.LOGGER.error("Hobby chat response threw", e);
                    }
                }
            }
        }

        // Periodically prune stale cooldown entries to keep maps small.
        // Entries older than ~5 minutes can't possibly still be gating anything (longest cooldown is 45s) so they're safe to drop.
        if ((now % 1200L) == 0) {
            long staleCutoff = now - (20L * 60 * 5);
            lastMercenaryResponseTick.entrySet().removeIf(e -> e.getValue() < staleCutoff);
            lastPlayerReceivedTick.entrySet().removeIf(e -> e.getValue() < staleCutoff);
        }
    }
}
