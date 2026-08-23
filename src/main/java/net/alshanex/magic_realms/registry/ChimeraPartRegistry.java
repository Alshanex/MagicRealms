package net.alshanex.magic_realms.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ChimeraMobDefinition;
import net.alshanex.magic_realms.data.ChimeraSlot;
import net.alshanex.magic_realms.network.SyncPartRegistryPayload;
import net.alshanex.magic_realms.util.chimera.EntityAttributeHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * Loads chimera part definitions from data packs.
 * <p>
 * Files are loaded from: data/{namespace}/chimera_parts/{entity_name}.json
 * <p>
 * On a dedicated server, this is populated by the datapack reload listener.
 * The definitions are then synced to clients via {@link SyncPartRegistryPayload} when they join, so the assembly GUI can display the available entities.
 */
public class ChimeraPartRegistry extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "chimera_parts";

    /**
     * The server-side (or integrated) instance, set when the reload listener is constructed in the mod constructor.
     */
    private static volatile ChimeraPartRegistry SERVER_INSTANCE;

    /**
     * A client-only instance used on dedicated server connections where SERVER_INSTANCE is null.
     */
    private static volatile ChimeraPartRegistry CLIENT_INSTANCE;

    private final Map<ResourceLocation, ChimeraMobDefinition> definitions = new HashMap<>();

    public ChimeraPartRegistry() {
        super(GSON, DIRECTORY);
        SERVER_INSTANCE = this;
    }

    /**
     * Returns the active registry instance. Prefers the server instance (which exists in singleplayer / integrated server), falls back to
     * the client instance (which exists when connected to a dedicated server).
     */
    public static ChimeraPartRegistry getInstance() {
        ChimeraPartRegistry inst = SERVER_INSTANCE;
        if (inst != null) return inst;
        return CLIENT_INSTANCE;
    }

    /**
     * Returns or lazily creates a client-side-only instance. Called from the client payload handler so the client always has a registry to
     * populate even when connected to a dedicated server.
     */
    public static ChimeraPartRegistry getOrCreateClientInstance() {
        // If the server instance exists (integrated server / singleplayer), reuse it - both sides share the same JVM.
        if (SERVER_INSTANCE != null) return SERVER_INSTANCE;

        if (CLIENT_INSTANCE == null) {
            // We don't register this as a reload listener — it's only
            // populated via network sync.
            CLIENT_INSTANCE = new ChimeraPartRegistry();
            // Undo the SERVER_INSTANCE assignment the constructor made
            SERVER_INSTANCE = null;
        }
        return CLIENT_INSTANCE;
    }

    /**
     * Called when disconnecting from a server to clean up the client instance.
     */
    public static void clearClientInstance() {
        CLIENT_INSTANCE = null;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        definitions.clear();
        int loaded = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                ChimeraMobDefinition def = ChimeraMobDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .getOrThrow(msg -> new IllegalStateException(
                                "Failed to parse chimera definition " + fileId + ": " + msg));

                // Validate that the entity type actually exists and has attributes
                if (EntityAttributeHelper.resolveEntityType(def.entity()).isEmpty()) {
                    MagicRealms.LOGGER.warn("Chimera definition {} references unknown or non-living entity: {}",
                            fileId, def.entity());
                    continue;
                }

                definitions.put(def.entity(), def);
                loaded++;
                MagicRealms.LOGGER.debug("Loaded chimera part definition for {}", def.entity());
            } catch (Exception e) {
                MagicRealms.LOGGER.error("Failed to load chimera part definition from {}: {}", fileId, e.getMessage());
            }
        }

        MagicRealms.LOGGER.info("Loaded {} chimera part definitions", loaded);
    }

    // Network Sync

    /**
     * Serialize all definitions to a CompoundTag for network transmission.
     * Each definition is stored under its entity ID string key.
     */
    public CompoundTag toNetwork() {
        CompoundTag root = new CompoundTag();
        for (Map.Entry<ResourceLocation, ChimeraMobDefinition> entry : definitions.entrySet()) {
            try {
                Tag encoded = ChimeraMobDefinition.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue())
                        .getOrThrow(msg -> new IllegalStateException("Failed to encode definition: " + msg));
                root.put(entry.getKey().toString(), encoded);
            } catch (Exception e) {
                MagicRealms.LOGGER.error("Failed to serialize chimera definition for {}: {}",
                        entry.getKey(), e.getMessage());
            }
        }
        return root;
    }

    /**
     * Deserialize definitions received from the server.
     * This replaces the client's current definitions entirely.
     * Called on the client side when receiving {@link SyncPartRegistryPayload}.
     */
    public void applyFromNetwork(CompoundTag registryData) {
        definitions.clear();
        int loaded = 0;

        for (String key : registryData.getAllKeys()) {
            try {
                ResourceLocation entityId = ResourceLocation.parse(key);
                Tag tag = registryData.get(key);

                ChimeraMobDefinition def = ChimeraMobDefinition.CODEC.parse(NbtOps.INSTANCE, tag)
                        .getOrThrow(msg -> new IllegalStateException(
                                "Failed to decode chimera definition " + key + ": " + msg));

                definitions.put(entityId, def);
                loaded++;
            } catch (Exception e) {
                MagicRealms.LOGGER.error("Failed to deserialize chimera definition '{}': {}",
                        key, e.getMessage());
            }
        }

        MagicRealms.LOGGER.info("Received {} chimera part definitions from server", loaded);
    }

    /**
     * Send all definitions to a specific player. Call this when a player joins the server.
     */
    public void sendToPlayer(ServerPlayer player) {
        CompoundTag data = toNetwork();
        PacketDistributor.sendToPlayer(player, new SyncPartRegistryPayload(data));
        MagicRealms.LOGGER.debug("Sent {} chimera part definitions to {}",
                definitions.size(), player.getName().getString());
    }

    /**
     * Send all definitions to all currently connected players.
     * Call this after a datapack reload (/reload command).
     */
    public void sendToAllPlayers() {
        CompoundTag data = toNetwork();
        PacketDistributor.sendToAllPlayers(new SyncPartRegistryPayload(data));
        MagicRealms.LOGGER.debug("Broadcast {} chimera part definitions to all players",
                definitions.size());
    }

    // Queries

    public Optional<ChimeraMobDefinition> getDefinition(ResourceLocation entityId) {
        return Optional.ofNullable(definitions.get(entityId));
    }

    public Set<ResourceLocation> getRegisteredEntities() {
        return Collections.unmodifiableSet(definitions.keySet());
    }

    public Collection<ChimeraMobDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public boolean hasDefinition(ResourceLocation entityId) {
        return definitions.containsKey(entityId);
    }

    /**
     * Get all entities that provide a specific chimera slot.
     */
    public List<ChimeraMobDefinition> getEntitiesWithSlot(ChimeraSlot slot) {
        return definitions.values().stream()
                .filter(def -> def.hasSlot(slot))
                .toList();
    }
}
