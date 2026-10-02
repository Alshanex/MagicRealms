package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.data.ContractedMercenariesSavedData;
import net.alshanex.magic_realms.data.ContractedMercenaryEntry;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.network.SyncContractedMercenariesPacket;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Keeps each level's {@link ContractedMercenariesSavedData} in step with the mercenaries' own {@link ContractData}
 * (which stays the source of truth) and pushes changes to the affected clients.
 */
@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ContractedMercenaryTracker {

    private static final int PRUNE_INTERVAL_TICKS = 100;

    private ContractedMercenaryTracker() {}

    // Public API

    /**
     * Inserts/updates this mercenary in its current level's data, or removes it if it no longer has an active contract.
     */
    public static void refresh(AbstractMercenaryEntity merc) {
        if (!(merc.level() instanceof ServerLevel level)) return;
        ContractedMercenariesSavedData data = ContractedMercenariesSavedData.get(level);

        ContractData contract = merc.getData(MRDataAttachments.CONTRACT_DATA);
        UUID contractorId = contract == null ? null : contract.getContractorUUID();
        if (contractorId == null || !contract.hasActiveContract(level)) {
            data.removeMercenary(merc.getUUID());
            return;
        }

        data.upsert(contractorId, ContractedMercenaryEntry.of(merc, contract));
    }

    /** Removes the mercenary from the data of the level it is currently in. */
    public static void remove(AbstractMercenaryEntity merc) {
        if (merc.level() instanceof ServerLevel level) {
            ContractedMercenariesSavedData.get(level).removeMercenary(merc.getUUID());
        }
    }

    public static boolean isContractedTo(AbstractMercenaryEntity merc, UUID playerId) {
        ContractData contract = merc.getData(MRDataAttachments.CONTRACT_DATA);
        return contract != null && contract.isContractor(playerId, merc.level());
    }

    /** Sends the player the full list for the level they are in right now. */
    public static void sendTo(ServerPlayer player) {
        ContractedMercenariesSavedData data = ContractedMercenariesSavedData.get(player.serverLevel());
        PacketDistributor.sendToPlayer(player, new SyncContractedMercenariesPacket(data.entriesFor(player.getUUID())));
    }

    // Entity events

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof AbstractMercenaryEntity merc)) return;

        Entity.RemovalReason reason = merc.getRemovalReason();
        if (reason == null) return;

        // Teleport re-creates the entity in the same level; the copy's join refreshes the entry in place (keeps card order).
        if (reason == Entity.RemovalReason.CHANGED_DIMENSION && MercenaryTeleportHandler.isRelocating(merc.getUUID())) return;

        if (reason.shouldDestroy() || reason == Entity.RemovalReason.CHANGED_DIMENSION) {
            // Dead/discarded, or moving to another level (which will add it back on join).
            ContractedMercenariesSavedData.get(level).removeMercenary(merc.getUUID());
        } else {
            // Chunk or player unload: record the exact final position for a later teleport.
            refresh(merc);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractMercenaryEntity merc)) return;
        refresh(merc);
    }

    // Player events: (re)send the list for the player's current level

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendTo(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendTo(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // Respawning can land the player in another dimension without a ChangedDimension event.
        if (event.getEntity() instanceof ServerPlayer player) sendTo(player);
    }

    // Server tick: expiry sweep + push changes

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        boolean prune = server.getTickCount() % PRUNE_INTERVAL_TICKS == 0;

        for (ServerLevel level : server.getAllLevels()) {
            ContractedMercenariesSavedData data = ContractedMercenariesSavedData.get(level);

            if (prune) data.pruneExpired(level.getGameTime());

            for (UUID playerId : data.drainSyncDirty()) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                // Players in other dimensions don't see this level's list; they'll get it when they arrive.
                if (player != null && player.serverLevel() == level) sendTo(player);
            }
        }
    }
}