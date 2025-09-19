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

    // Map: WorldKey -> Map<EntityType, ExclusiveMercenaryInfo> for tracking instances across dimensions
    private static final Map<String, Map<EntityType<?>, ExclusiveMercenaryInfo>> WORLD_EXCLUSIVE_INSTANCES = new ConcurrentHashMap<>();

    // Track entity info across dimensions
    private static class ExclusiveMercenaryInfo {
        final UUID entityUUID;
        String currentDimension;

        ExclusiveMercenaryInfo(UUID entityUUID, String currentDimension) {
            this.entityUUID = entityUUID;
            this.currentDimension = currentDimension;
        }
    }

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
        Map<EntityType<?>, ExclusiveMercenaryInfo> instances = WORLD_EXCLUSIVE_INSTANCES.getOrDefault(worldKey, Collections.emptyMap());

        Map<EntityType<?>, UUID> result = new HashMap<>();
        instances.forEach((type, info) -> result.put(type, info.entityUUID));
        return result;
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
        String dimensionKey = getDimensionKey(serverLevel);
        EntityType<?> entityType = mercenary.getType();
        UUID entityUUID = mercenary.getUUID();

        // Check if this entity is already tracked (dimension change vs new spawn)
        Map<EntityType<?>, ExclusiveMercenaryInfo> worldInstances = WORLD_EXCLUSIVE_INSTANCES.get(worldKey);
        if (worldInstances != null && worldInstances.containsKey(entityType)) {
            ExclusiveMercenaryInfo existing = worldInstances.get(entityType);
            if (existing.entityUUID.equals(entityUUID)) {
                // Same entity changing dimensions - update location
                existing.currentDimension = dimensionKey;
                MagicRealms.LOGGER.debug("Exclusive mercenary {} ({}) moved to dimension {} in world {}",
                        entityType.getDescriptionId(), entityUUID, dimensionKey, worldKey);
                return;
            }
        }

        // New entity spawn - add to registry
        WORLD_EXCLUSIVE_REGISTRY.computeIfAbsent(worldKey, k -> ConcurrentHashMap.newKeySet()).add(entityType);
        WORLD_EXCLUSIVE_INSTANCES.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>())
                .put(entityType, new ExclusiveMercenaryInfo(entityUUID, dimensionKey));

        MagicRealms.LOGGER.debug("Exclusive mercenary {} ({}) spawned in dimension {} of world {} - now tracking",
                entityType.getDescriptionId(), entityUUID, dimensionKey, worldKey);
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
        String dimensionKey = getDimensionKey(serverLevel);
        EntityType<?> entityType = mercenary.getType();
        UUID entityUUID = mercenary.getUUID();

        // Get tracking info
        Map<EntityType<?>, ExclusiveMercenaryInfo> worldInstances = WORLD_EXCLUSIVE_INSTANCES.get(worldKey);
        if (worldInstances == null || !worldInstances.containsKey(entityType)) {
            return; // Not tracked
        }

        ExclusiveMercenaryInfo info = worldInstances.get(entityType);
        if (!info.entityUUID.equals(entityUUID)) {
            return; // Different entity
        }

        // Check if entity is actually removed/dead vs changing dimensions
        if (mercenary.isRemoved() || !mercenary.isAlive()) {
            // Entity is permanently gone - remove from all tracking
            Set<EntityType<?>> worldExclusives = WORLD_EXCLUSIVE_REGISTRY.get(worldKey);
            if (worldExclusives != null) {
                worldExclusives.remove(entityType);
                if (worldExclusives.isEmpty()) {
                    WORLD_EXCLUSIVE_REGISTRY.remove(worldKey);
                }
            }

            worldInstances.remove(entityType);
            if (worldInstances.isEmpty()) {
                WORLD_EXCLUSIVE_INSTANCES.remove(worldKey);
            }

            MagicRealms.LOGGER.debug("Exclusive mercenary {} ({}) permanently removed from world {} - no longer tracking",
                    entityType.getDescriptionId(), entityUUID, worldKey);
        } else {
            // Entity is just changing dimensions - update will happen in join event
            MagicRealms.LOGGER.debug("Exclusive mercenary {} ({}) leaving dimension {} in world {} (dimension change)",
                    entityType.getDescriptionId(), entityUUID, dimensionKey, worldKey);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        // Only clean up on server side
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        String worldKey = getWorldKey(serverLevel);

        // Clean up tracking for this world when server shuts down
        Set<EntityType<?>> removed = WORLD_EXCLUSIVE_REGISTRY.remove(worldKey);
        Map<EntityType<?>, ExclusiveMercenaryInfo> removedInstances = WORLD_EXCLUSIVE_INSTANCES.remove(worldKey);

        if (removed != null && !removed.isEmpty()) {
            MagicRealms.LOGGER.debug("Cleaned up exclusive mercenary tracking for world {} - removed {} types",
                    worldKey, removed.size());
        }
    }

    /**
     * Create a unique key for the entire world (across all dimensions)
     */
    private static String getWorldKey(ServerLevel level) {
        return level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .getFileName().toString();
    }

    /**
     * Get the dimension-specific key for logging/debugging
     */
    private static String getDimensionKey(ServerLevel level) {
        return level.dimension().location().toString();
    }
}