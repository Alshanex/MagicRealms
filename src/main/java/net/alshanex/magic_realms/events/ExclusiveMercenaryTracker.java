package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ExclusiveMercenaryTracker {

    // Map: WorldKey -> Set<EntityType> of exclusive mercenaries present
    private static final Map<String, Set<EntityType<?>>> WORLD_EXCLUSIVE_REGISTRY = new ConcurrentHashMap<>();

    // Map: WorldKey -> Map<EntityType, UUID> for tracking specific exclusive mercenary instances
    private static final Map<String, Map<EntityType<?>, UUID>> WORLD_EXCLUSIVE_INSTANCES = new ConcurrentHashMap<>();

    /**
     * Check if a specific exclusive mercenary type can be spawned in the given world
     */
    public static boolean canSpawnExclusiveMercenary(ServerLevel level, EntityType<?> entityType) {
        String worldKey = getWorldKey(level);
        Set<EntityType<?>> exclusives = WORLD_EXCLUSIVE_REGISTRY.get(worldKey);

        boolean canSpawn = exclusives == null || !exclusives.contains(entityType);

        if (!canSpawn) {
            MagicRealms.LOGGER.debug("Cannot spawn exclusive mercenary {} - already exists in world {}",
                    entityType.getDescriptionId(), worldKey);
        }

        return canSpawn;
    }

    /**
     * Get all exclusive mercenary types that can be spawned in the given world
     */
    public static List<EntityType<?>> getAvailableExclusiveMercenaries(ServerLevel level, List<EntityType<?>> allExclusiveTypes) {
        String worldKey = getWorldKey(level);
        Set<EntityType<?>> existingTypes = WORLD_EXCLUSIVE_REGISTRY.getOrDefault(worldKey, Collections.emptySet());

        return allExclusiveTypes.stream()
                .filter(type -> !existingTypes.contains(type))
                .toList();
    }

    /**
     * Get the count of exclusive mercenaries in a world
     */
    public static int getExclusiveMercenaryCount(ServerLevel level) {
        String worldKey = getWorldKey(level);
        Set<EntityType<?>> exclusives = WORLD_EXCLUSIVE_REGISTRY.get(worldKey);
        return exclusives != null ? exclusives.size() : 0;
    }

    /**
     * Get detailed info about exclusive mercenaries in a world
     */
    public static Map<EntityType<?>, UUID> getExclusiveMercenaryInstances(ServerLevel level) {
        String worldKey = getWorldKey(level);
        return WORLD_EXCLUSIVE_INSTANCES.getOrDefault(worldKey, Collections.emptyMap());
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Only track on server side
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Only track AbstractMercenaryEntity that are exclusive
        if (!(event.getEntity() instanceof AbstractMercenaryEntity mercenary) || !mercenary.isExclusiveMercenary()) {
            return;
        }

        String worldKey = getWorldKey(serverLevel);
        EntityType<?> entityType = mercenary.getType();
        UUID entityUUID = mercenary.getUUID();

        // Add to registry
        WORLD_EXCLUSIVE_REGISTRY.computeIfAbsent(worldKey, k -> ConcurrentHashMap.newKeySet()).add(entityType);
        WORLD_EXCLUSIVE_INSTANCES.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>()).put(entityType, entityUUID);

        MagicRealms.LOGGER.debug("Exclusive mercenary {} ({}) joined world {} - now tracking",
                entityType.getDescriptionId(), entityUUID, worldKey);
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        // Only track on server side
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Only track AbstractMercenaryEntity that are exclusive
        if (!(event.getEntity() instanceof AbstractMercenaryEntity mercenary) || !mercenary.isExclusiveMercenary()) {
            return;
        }

        String worldKey = getWorldKey(serverLevel);
        EntityType<?> entityType = mercenary.getType();
        UUID entityUUID = mercenary.getUUID();

        // Remove from registry
        Set<EntityType<?>> worldExclusives = WORLD_EXCLUSIVE_REGISTRY.get(worldKey);
        if (worldExclusives != null) {
            worldExclusives.remove(entityType);
            if (worldExclusives.isEmpty()) {
                WORLD_EXCLUSIVE_REGISTRY.remove(worldKey);
            }
        }

        Map<EntityType<?>, UUID> worldInstances = WORLD_EXCLUSIVE_INSTANCES.get(worldKey);
        if (worldInstances != null) {
            // Only remove if it's the same instance (in case of rare UUID conflicts)
            if (Objects.equals(worldInstances.get(entityType), entityUUID)) {
                worldInstances.remove(entityType);
                if (worldInstances.isEmpty()) {
                    WORLD_EXCLUSIVE_INSTANCES.remove(worldKey);
                }
            }
        }

        MagicRealms.LOGGER.debug("Exclusive mercenary {} ({}) left world {} - no longer tracking",
                entityType.getDescriptionId(), entityUUID, worldKey);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        // Only clean up on server side
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        String worldKey = getWorldKey(serverLevel);

        // Clean up tracking for this world
        Set<EntityType<?>> removed = WORLD_EXCLUSIVE_REGISTRY.remove(worldKey);
        Map<EntityType<?>, UUID> removedInstances = WORLD_EXCLUSIVE_INSTANCES.remove(worldKey);

        if (removed != null && !removed.isEmpty()) {
            MagicRealms.LOGGER.debug("Cleaned up exclusive mercenary tracking for world {} - removed {} types",
                    worldKey, removed.size());
        }
    }

    /**
     * Create a unique key for the world
     */
    private static String getWorldKey(ServerLevel level) {
        return level.dimension().location().toString();
    }
}
