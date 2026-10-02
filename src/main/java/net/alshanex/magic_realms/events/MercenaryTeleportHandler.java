package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractedMercenariesSavedData;
import net.alshanex.magic_realms.data.ContractedMercenaryEntry;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Brings a contracted mercenary next to its contractor. Only mercenaries in the player's <b>current</b> level can be
 * requested, so this never crosses dimensions.
 *
 * <p>Mercenaries in entity-ticking chunks are moved immediately. Otherwise a temporary chunk ticket is placed on their
 * chunk and we poll each tick until the mercenary is loaded <b>and ticking</b> (its data becomes readable earlier than
 * that, but moving it in that hidden state doesn't take). If it never appears, the stale entry is dropped; the exact
 * position is always recorded when a mercenary unloads, so a miss means it's really gone.
 */
@EventBusSubscriber(modid = MagicRealms.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class MercenaryTeleportHandler {

    /** Self-expiring, so a crash mid-fetch can't leave a chunk loaded forever. */
    private static final TicketType<ChunkPos> FETCH_TICKET =
            TicketType.create(MagicRealms.MODID + ":mercenary_fetch", Comparator.comparingLong(ChunkPos::toLong), 300);

    /** Radius 2 => ticket level 31 at the centre chunk, i.e. entity-ticking. */
    private static final int TICKET_RADIUS = 2;
    private static final int FETCH_TIMEOUT_TICKS = 200;
    private static final int COOLDOWN_TICKS = 40;
    private static final int[] DY_ORDER = {0, 1, -1};

    private static final Map<UUID, PendingFetch> PENDING = new HashMap<>();   // keyed by player
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();      // keyed by player

    private static final class PendingFetch {
        final UUID playerId;
        final UUID mercenaryId;
        final String name;
        final ServerLevel level;
        final ChunkPos chunk;
        int ticksLeft = FETCH_TIMEOUT_TICKS;

        PendingFetch(UUID playerId, UUID mercenaryId, String name, ServerLevel level, ChunkPos chunk) {
            this.playerId = playerId;
            this.mercenaryId = mercenaryId;
            this.name = name;
            this.level = level;
            this.chunk = chunk;
        }
    }

    private MercenaryTeleportHandler() {}

    public static void requestTeleport(ServerPlayer player, UUID mercenaryId) {
        MinecraftServer server = player.server;

        long now = server.getTickCount();
        Long last = LAST_REQUEST.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) {
            player.displayClientMessage(Component.translatable("message.magic_realms.mercenaries.cooldown")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        LAST_REQUEST.put(player.getUUID(), now);

        // Only mercenaries listed for this player in this level can be requested.
        ServerLevel level = player.serverLevel();
        ContractedMercenaryEntry entry = ContractedMercenariesSavedData.get(level).getEntry(player.getUUID(), mercenaryId);
        if (entry == null) return;

        Entity loaded = level.getEntity(mercenaryId);
        if (loaded != null && isReady(level, loaded)) {
            tryTeleport(player, loaded, entry.name());
            return;
        }

        if (PENDING.containsKey(player.getUUID())) return; // one fetch at a time per player

        // Not loaded, or loaded but not ticking (e.g. at the edge of view distance): ticket its chunk and wait.
        ChunkPos chunk = loaded != null ? loaded.chunkPosition() : new ChunkPos(entry.lastPos());
        level.getChunkSource().addRegionTicket(FETCH_TICKET, chunk, TICKET_RADIUS, chunk);
        PENDING.put(player.getUUID(), new PendingFetch(player.getUUID(), mercenaryId, entry.name(), level, chunk));

        player.displayClientMessage(Component.translatable("message.magic_realms.mercenaries.locating", entry.name())
                .withStyle(ChatFormatting.GRAY), true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        MinecraftServer server = event.getServer();

        Iterator<PendingFetch> it = PENDING.values().iterator();
        while (it.hasNext()) {
            PendingFetch fetch = it.next();

            ServerPlayer player = server.getPlayerList().getPlayer(fetch.playerId);
            if (player == null || player.serverLevel() != fetch.level) {
                // Player left, or changed dimension mid-fetch: cancel quietly.
                release(fetch);
                it.remove();
                continue;
            }

            // The entity is readable as soon as its data loads from disk, before its chunk is fully loaded. Moving it
            // while it's in that hidden, non-ticking state doesn't stick, so wait until its position is entity-ticking.
            Entity found = fetch.level.getEntity(fetch.mercenaryId);
            if (found != null && isReady(fetch.level, found)) {
                tryTeleport(player, found, fetch.name);
                release(fetch);
                it.remove();
                continue;
            }

            if (--fetch.ticksLeft <= 0) {
                release(fetch);
                it.remove();
                if (found != null) {
                    // It exists, its chunk just never finished loading in time: keep the entry, let them retry.
                    player.displayClientMessage(Component.translatable(
                            "message.magic_realms.mercenaries.not_found", fetch.name).withStyle(ChatFormatting.RED), true);
                } else {
                    ContractedMercenariesSavedData.get(fetch.level).removeMercenary(fetch.mercenaryId);
                    player.displayClientMessage(Component.translatable(
                            "message.magic_realms.mercenaries.not_found", fetch.name).withStyle(ChatFormatting.RED), false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
        LAST_REQUEST.clear();
    }

    // Internals

    private static void tryTeleport(ServerPlayer player, Entity entity, String fallbackName) {
        if (!(entity instanceof AbstractMercenaryEntity merc)
                || !ContractedMercenaryTracker.isContractedTo(merc, player.getUUID())) {
            // Stale entry: let the tracker re-index or drop it based on the real contract.
            if (entity instanceof AbstractMercenaryEntity merc) {
                ContractedMercenaryTracker.refresh(merc);
            } else {
                ContractedMercenariesSavedData.get(player.serverLevel()).removeMercenary(entity.getUUID());
            }
            player.displayClientMessage(Component.translatable(
                    "message.magic_realms.mercenaries.no_longer_contracted", fallbackName).withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!merc.isAlive()) return;

        ServerLevel level = player.serverLevel();
        Vec3 dest = findSafeSpot(level, player, merc);

        if (merc.isSittingInChair()) merc.unsitFromChair();

        // Summoned to the player = follow them. Same as the contract screen's toggle (follow is just "not patrolling");
        // the patrol centre is reset like clearContract() does, so an old post isn't remembered.
        boolean wasPatrolling = merc.isPatrolMode();
        if (wasPatrolling) {
            merc.setPatrolMode(false);
            merc.setPatrolPosition(BlockPos.ZERO);
        }

        // Drop whatever it was fighting at its old location, otherwise it would run straight back to it.
        merc.setTarget(null);
        merc.getNavigation().stop();
        merc.fallDistance = 0;

        AbstractMercenaryEntity arrived = relocate(level, merc, dest, player.getYRot() + 180f);

        if (arrived.isRemoved() || arrived.position().distanceToSqr(dest) > 4.0) {
            // Relocation was rejected; it stayed (or was put back) where it was.
            player.displayClientMessage(Component.translatable(
                    "message.magic_realms.mercenaries.not_found", arrived.getEntityName()).withStyle(ChatFormatting.RED), true);
            ContractedMercenaryTracker.refresh(arrived);
            return;
        }

        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.8f, 1.0f);

        // Record the new position right away rather than waiting for the next tick refresh.
        ContractedMercenaryTracker.refresh(arrived);
    }

    /** Mercenaries currently being re-created by {@link #relocate}; the tracker must not drop their entry. */
    private static final java.util.Set<UUID> RELOCATING = new java.util.HashSet<>();

    public static boolean isRelocating(UUID uuid) {
        return RELOCATING.contains(uuid);
    }

    /**
     * Moves the mercenary the way vanilla moves entities through portals: a fresh copy is created at the destination from
     * the old one's full save data (same UUID, inventory, attachments, contract, flags), the old instance is removed, and
     * the copy is added.
     */
    private static AbstractMercenaryEntity relocate(ServerLevel level, AbstractMercenaryEntity merc, Vec3 dest, float yRot) {
        Entity created = merc.getType().create(level);
        if (!(created instanceof AbstractMercenaryEntity copy)) {
            // Shouldn't happen; fall back to an in-place move.
            merc.teleportTo(dest.x, dest.y, dest.z);
            return merc;
        }

        UUID id = merc.getUUID();
        Vec3 origin = merc.position();
        RELOCATING.add(id);
        try {
            copy.restoreFrom(merc);                     // same as loading it from disk: NBT + attachments + UUID
            copy.moveTo(dest.x, dest.y, dest.z, yRot, 0f);
            copy.setYHeadRot(yRot);
            copy.setYBodyRot(yRot);
            copy.setDeltaMovement(Vec3.ZERO);

            // Must come first: the copy has the same UUID and would be rejected while the original is still registered.
            merc.remove(Entity.RemovalReason.CHANGED_DIMENSION); // not treated as a death anywhere

            // addWithUUID is ServerLevel's entry point for "unnatural" arrivals (summon, interdimensional teleport).
            if (level.addWithUUID(copy)) {
                return copy;
            }

            // Rejected (e.g. a cancelled EntityJoinLevelEvent). The original is already removed, so put a copy back
            // where it was rather than losing the mercenary.
            MagicRealms.LOGGER.error("Failed to relocate mercenary {} ({}); restoring it at its original position",
                    merc.getEntityName(), id);
            Entity restored = merc.getType().create(level);
            if (restored instanceof AbstractMercenaryEntity back) {
                back.restoreFrom(merc);
                back.moveTo(origin.x, origin.y, origin.z, merc.getYRot(), merc.getXRot());
                if (level.addWithUUID(back)) return back;
            }
            MagicRealms.LOGGER.error("Could not restore mercenary {} ({}) either", merc.getEntityName(), id);
            return copy;
        } finally {
            RELOCATING.remove(id);
        }
    }

    private static Vec3 findSafeSpot(ServerLevel level, ServerPlayer player, AbstractMercenaryEntity merc) {
        EntityDimensions dims = merc.getType().getDimensions();
        BlockPos origin = player.blockPosition();

        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue; // ring only
                    for (int dy : DY_ORDER) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        if (isSafe(level, dims, pos)) return Vec3.atBottomCenterOf(pos);
                    }
                }
            }
        }
        return player.position();
    }

    private static boolean isSafe(ServerLevel level, EntityDimensions dims, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return false;
        if (!level.getFluidState(pos).isEmpty()) return false;
        AABB box = dims.makeBoundingBox(Vec3.atBottomCenterOf(pos));
        return level.noCollision(box);
    }

    /** True once the entity is fully loaded and ticking, i.e. safe to move. */
    private static boolean isReady(ServerLevel level, Entity entity) {
        return entity.isAlive() && level.isPositionEntityTicking(entity.blockPosition());
    }

    private static void release(PendingFetch fetch) {
        fetch.level.getChunkSource().removeRegionTicket(FETCH_TICKET, fetch.chunk, TICKET_RADIUS, fetch.chunk);
    }
}