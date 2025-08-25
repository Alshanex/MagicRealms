package net.alshanex.magic_realms.util.chat_system;

import com.google.gson.*;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Centralized manager for all entity AI brains
 * Handles persistence, memory management, and shared knowledge
 */
public class EntityAIManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static EntityAIManager INSTANCE;

    private final Map<UUID, EntityAIBrain> activeBrains = new ConcurrentHashMap<>();
    private final KnowledgeBase globalKnowledge = new KnowledgeBase();
    private Path storagePath;
    private MinecraftServer server;

    // Auto-save scheduler
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Configuration
    private static final int AUTO_SAVE_INTERVAL_MINUTES = 5;
    private static final int MAX_CACHED_BRAINS = 100;
    private static final long BRAIN_CACHE_EXPIRE_MS = 30 * 60 * 1000; // 30 minutes

    private EntityAIManager(MinecraftServer server) {
        this.server = server;
        this.storagePath = server.getWorldPath(LevelResource.ROOT).resolve("magic_realms_ai");

        try {
            Files.createDirectories(storagePath);
            MagicRealms.LOGGER.info("Created AI storage directory at: {}", storagePath);
        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to create AI storage directory", e);
        }

        // Load existing data
        loadGlobalKnowledge();
        loadAllBrains();

        // Schedule auto-save
        scheduler.scheduleAtFixedRate(this::autoSave,
                AUTO_SAVE_INTERVAL_MINUTES,
                AUTO_SAVE_INTERVAL_MINUTES,
                TimeUnit.MINUTES);

        MagicRealms.LOGGER.info("Entity AI Manager initialized with {} cached brains", activeBrains.size());
    }

    public static void initialize(MinecraftServer server) {
        if (INSTANCE == null) {
            INSTANCE = new EntityAIManager(server);
            KnowledgeBase.setStoragePath(server);
        }
    }

    public static EntityAIManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("EntityAIManager not initialized! Call initialize() first.");
        }
        return INSTANCE;
    }

    /**
     * Get or create an AI brain for an entity
     */
    public static EntityAIBrain getBrain(RandomHumanEntity entity) {
        return getInstance().getOrCreateBrain(entity);
    }

    private EntityAIBrain getOrCreateBrain(RandomHumanEntity entity) {
        UUID entityId = entity.getUUID();

        // Check if brain exists in cache
        EntityAIBrain brain = activeBrains.get(entityId);
        if (brain != null) {
            return brain;
        }

        // Try to load from disk
        brain = loadBrain(entityId, entity);
        if (brain != null) {
            activeBrains.put(entityId, brain);
            return brain;
        }

        // Create new brain
        brain = new EntityAIBrain(entityId, entity);
        activeBrains.put(entityId, brain);

        // Clean cache if too large
        cleanCache();

        MagicRealms.LOGGER.info("Created new AI brain for entity: {} ({})",
                entity.getEntityName(), entityId);

        return brain;
    }

    /**
     * Save a specific brain to disk
     */
    private void saveBrain(UUID entityId, EntityAIBrain brain) {
        try {
            Path brainFile = storagePath.resolve("brains").resolve(entityId.toString() + ".json");
            Files.createDirectories(brainFile.getParent());

            JsonObject json = brain.serialize();

            try (FileWriter writer = new FileWriter(brainFile.toFile())) {
                GSON.toJson(json, writer);
            }

            MagicRealms.LOGGER.debug("Saved brain for entity: {}", entityId);

        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to save brain for entity: {}", entityId, e);
        }
    }

    /**
     * Load a brain from disk
     */
    private EntityAIBrain loadBrain(UUID entityId, RandomHumanEntity entity) {
        try {
            Path brainFile = storagePath.resolve("brains").resolve(entityId.toString() + ".json");

            if (!Files.exists(brainFile)) {
                return null;
            }

            try (FileReader reader = new FileReader(brainFile.toFile())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                EntityAIBrain brain = EntityAIBrain.deserialize(json, entity);

                MagicRealms.LOGGER.debug("Loaded brain for entity: {}", entityId);
                return brain;
            }

        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to load brain for entity: {}", entityId, e);
            return null;
        }
    }

    /**
     * Load all brains from disk (called on startup)
     */
    private void loadAllBrains() {
        try {
            Path brainsDir = storagePath.resolve("brains");
            if (!Files.exists(brainsDir)) {
                return;
            }

            Files.list(brainsDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String filename = path.getFileName().toString();
                            String uuidStr = filename.substring(0, filename.length() - 5);
                            UUID entityId = UUID.fromString(uuidStr);

                            // We can't fully load without the entity, so just note it exists
                            MagicRealms.LOGGER.debug("Found stored brain for entity: {}", entityId);

                        } catch (Exception e) {
                            MagicRealms.LOGGER.warn("Failed to process brain file: {}", path, e);
                        }
                    });

        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to load brain directory", e);
        }
    }

    /**
     * Save all active brains to disk
     */
    public static void saveAll() {
        if (INSTANCE != null) {
            INSTANCE.saveAllBrains();
        }
    }

    private void saveAllBrains() {
        MagicRealms.LOGGER.info("Saving {} active AI brains...", activeBrains.size());

        activeBrains.forEach(this::saveBrain);
        globalKnowledge.save();

        MagicRealms.LOGGER.info("Saved all AI data");
    }

    /**
     * Auto-save task
     */
    private void autoSave() {
        try {
            saveAllBrains();
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Auto-save failed", e);
        }
    }

    /**
     * Clean the cache of old/unused brains
     */
    private void cleanCache() {
        if (activeBrains.size() <= MAX_CACHED_BRAINS) {
            return;
        }

        // Save and remove least recently used brains
        List<Map.Entry<UUID, EntityAIBrain>> entries = new ArrayList<>(activeBrains.entrySet());

        // Sort by last access time (would need to track this in EntityAIBrain)
        // For now, just remove random ones
        int toRemove = activeBrains.size() - MAX_CACHED_BRAINS;

        for (int i = 0; i < toRemove && i < entries.size(); i++) {
            Map.Entry<UUID, EntityAIBrain> entry = entries.get(i);
            saveBrain(entry.getKey(), entry.getValue());
            activeBrains.remove(entry.getKey());
        }

        MagicRealms.LOGGER.debug("Cleaned cache, removed {} brains", toRemove);
    }

    /**
     * Load global knowledge base
     */
    private void loadGlobalKnowledge() {
        globalKnowledge.load();
    }

    /**
     * Get the global knowledge base
     */
    public static KnowledgeBase getGlobalKnowledge() {
        return getInstance().globalKnowledge;
    }

    /**
     * Shutdown the manager
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        saveAllBrains();
    }

    /**
     * Get statistics about the AI system
     */
    public AISystemStats getStats() {
        AISystemStats stats = new AISystemStats();
        stats.totalBrainsLoaded = activeBrains.size();
        stats.totalKnowledgeEntries = globalKnowledge.getKnowledgeCount();
        stats.storageUsedMB = calculateStorageUsed();
        return stats;
    }

    private double calculateStorageUsed() {
        try {
            return Files.walk(storagePath)
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum() / (1024.0 * 1024.0);
        } catch (IOException e) {
            return 0;
        }
    }

    public static class AISystemStats {
        public int totalBrainsLoaded;
        public int totalKnowledgeEntries;
        public double storageUsedMB;

        @Override
        public String toString() {
            return String.format("AI System Stats: %d brains loaded, %d knowledge entries, %.2f MB storage used",
                    totalBrainsLoaded, totalKnowledgeEntries, storageUsedMB);
        }
    }
}
