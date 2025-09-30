package net.alshanex.magic_realms.raid;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.alshanex.magic_realms.entity.random.hostile.HostileRandomHumanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class CustomRaid {
    private static final int MAX_WAVES = 10;
    private static final int WAVE_COOLDOWN_TICKS = 200; // 10 seconds between waves
    private static final int SPAWN_RADIUS = 32;
    private static final int MAX_SPAWN_ATTEMPTS = 20;

    private final Map<Integer, Set<HostileRandomHumanEntity>> waveEntities = Maps.newHashMap();
    private final ServerLevel level;
    private final UUID targetPlayerUUID;
    private final int id;
    private BlockPos center;
    private int currentWave = 0;
    private int cooldownTicks = WAVE_COOLDOWN_TICKS;
    private boolean active = true;
    private final ServerBossEvent bossBar;
    private RaidStatus status = RaidStatus.ONGOING;
    private float totalHealth = 0.0F;

    public CustomRaid(int id, ServerLevel level, BlockPos center, Player targetPlayer) {
        this.id = id;
        this.level = level;
        this.center = center;
        this.targetPlayerUUID = targetPlayer.getUUID();
        this.bossBar = new ServerBossEvent(
                Component.literal("Hostile Human Raid - Wave 0/" + MAX_WAVES),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.NOTCHED_10
        );
        this.bossBar.setProgress(0.0F);
    }

    public CustomRaid(ServerLevel level, CompoundTag compound) {
        this.level = level;
        this.id = compound.getInt("Id");
        this.active = compound.getBoolean("Active");
        this.currentWave = compound.getInt("CurrentWave");
        this.cooldownTicks = compound.getInt("CooldownTicks");
        this.totalHealth = compound.getFloat("TotalHealth");
        this.center = new BlockPos(
                compound.getInt("CX"),
                compound.getInt("CY"),
                compound.getInt("CZ")
        );
        this.targetPlayerUUID = compound.getUUID("TargetPlayer");
        this.status = RaidStatus.valueOf(compound.getString("Status"));

        this.bossBar = new ServerBossEvent(
                Component.literal("Hostile Human Raid - Wave " + currentWave + "/" + MAX_WAVES),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.NOTCHED_10
        );
    }

    public void tick() {
        if (!active || status != RaidStatus.ONGOING) {
            if (status == RaidStatus.VICTORY || status == RaidStatus.DEFEAT) {
                handleRaidEnd();
            }
            return;
        }

        ServerPlayer targetPlayer = level.getServer().getPlayerList().getPlayer(targetPlayerUUID);
        if (targetPlayer == null || !targetPlayer.isAlive()) {
            // Target player is dead or disconnected - raid victory for entities
            status = RaidStatus.DEFEAT;
            return;
        }

        // Update boss bar for nearby players
        updateBossBar(targetPlayer);

        // Check if all entities in current wave are dead
        int aliveCount = countAliveEntities();

        if (aliveCount == 0) {
            if (currentWave >= MAX_WAVES) {
                // All waves completed, player wins
                status = RaidStatus.VICTORY;
                return;
            }

            // Start cooldown for next wave
            if (cooldownTicks > 0) {
                cooldownTicks--;
                updateCooldownProgress();
            } else {
                // Spawn next wave
                spawnNextWave();
                cooldownTicks = WAVE_COOLDOWN_TICKS;
            }
        } else {
            updateHealthProgress();
        }

        // Clean up dead entities
        cleanupDeadEntities();
    }

    private void spawnNextWave() {
        currentWave++;

        Set<HostileRandomHumanEntity> waveSet = Sets.newHashSet();
        waveEntities.put(currentWave, waveSet);

        // Spawn 3-5 entities per wave, increasing with wave number
        int entitiesToSpawn = 3 + (currentWave / 2);
        entitiesToSpawn = Math.min(entitiesToSpawn, 8); // Cap at 8 per wave

        totalHealth = 0.0F;

        for (int i = 0; i < entitiesToSpawn; i++) {
            BlockPos spawnPos = findSpawnPosition();
            if (spawnPos != null) {
                HostileRandomHumanEntity entity = new HostileRandomHumanEntity(level, currentWave);
                entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

                entity.finalizeSpawn(
                        level,
                        level.getCurrentDifficultyAt(spawnPos),
                        net.minecraft.world.entity.MobSpawnType.EVENT,
                        null
                );

                // Set target to the player
                ServerPlayer targetPlayer = level.getServer().getPlayerList().getPlayer(targetPlayerUUID);
                if (targetPlayer != null) {
                    entity.setTarget(targetPlayer);
                }

                level.addFreshEntity(entity);
                waveSet.add(entity);
                totalHealth += entity.getMaxHealth();
            }
        }

        bossBar.setName(Component.literal("Hostile Human Raid - Wave " + currentWave + "/" + MAX_WAVES));
    }

    private BlockPos findSpawnPosition() {
        for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
            float angle = level.random.nextFloat() * (float) (Math.PI * 2);
            int x = center.getX() + Mth.floor(Mth.cos(angle) * SPAWN_RADIUS);
            int z = center.getZ() + Mth.floor(Mth.sin(angle) * SPAWN_RADIUS);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

            BlockPos pos = new BlockPos(x, y, z);

            AABB boundingBox = new AABB(
                    x - 0.3, y, z - 0.3,
                    x + 0.3, y + 1.8, z + 0.3
            );

            if (level.noCollision(null, boundingBox) && level.isPositionEntityTicking(pos)) {
                return pos;
            }
        }
        return null;
    }

    private int countAliveEntities() {
        int count = 0;
        for (Set<HostileRandomHumanEntity> wave : waveEntities.values()) {
            for (HostileRandomHumanEntity entity : wave) {
                if (entity.isAlive() && !entity.isRemoved()) {
                    count++;
                }
            }
        }
        return count;
    }

    private float getCurrentHealth() {
        float health = 0.0F;
        for (Set<HostileRandomHumanEntity> wave : waveEntities.values()) {
            for (HostileRandomHumanEntity entity : wave) {
                if (entity.isAlive() && !entity.isRemoved()) {
                    health += entity.getHealth();
                }
            }
        }
        return health;
    }

    private void cleanupDeadEntities() {
        for (Set<HostileRandomHumanEntity> wave : waveEntities.values()) {
            wave.removeIf(entity -> !entity.isAlive() || entity.isRemoved());
        }
    }

    private void updateBossBar(ServerPlayer targetPlayer) {
        // Add target player to boss bar
        if (!bossBar.getPlayers().contains(targetPlayer)) {
            bossBar.addPlayer(targetPlayer);
        }

        // Add nearby players to boss bar
        for (ServerPlayer player : level.players()) {
            double distance = player.distanceToSqr(Vec3.atCenterOf(center));
            if (distance < 4096.0) { // 64 block radius
                if (!bossBar.getPlayers().contains(player)) {
                    bossBar.addPlayer(player);
                }
            } else {
                if (bossBar.getPlayers().contains(player) && !player.getUUID().equals(targetPlayerUUID)) {
                    bossBar.removePlayer(player);
                }
            }
        }
    }

    private void updateCooldownProgress() {
        float progress = 1.0F - ((float) cooldownTicks / (float) WAVE_COOLDOWN_TICKS);
        bossBar.setProgress(Mth.clamp(progress, 0.0F, 1.0F));
    }

    private void updateHealthProgress() {
        if (totalHealth > 0) {
            float progress = getCurrentHealth() / totalHealth;
            bossBar.setProgress(Mth.clamp(progress, 0.0F, 1.0F));
        }
    }

    private void handleRaidEnd() {
        if (status == RaidStatus.VICTORY) {
            bossBar.setName(Component.literal("Victory! Raid Completed"));
            bossBar.setColor(BossEvent.BossBarColor.GREEN);
        } else if (status == RaidStatus.DEFEAT) {
            bossBar.setName(Component.literal("Defeat! You have been slain"));
            bossBar.setColor(BossEvent.BossBarColor.RED);
        }

        // Keep boss bar visible for a few seconds then remove
        // The manager should handle removal after ticks
    }

    public void stop() {
        this.active = false;
        this.bossBar.removeAllPlayers();

        // Remove all raid entities
        for (Set<HostileRandomHumanEntity> wave : waveEntities.values()) {
            for (HostileRandomHumanEntity entity : wave) {
                if (entity.isAlive()) {
                    entity.discard();
                }
            }
        }
    }

    public CompoundTag save(CompoundTag compound) {
        compound.putInt("Id", id);
        compound.putBoolean("Active", active);
        compound.putInt("CurrentWave", currentWave);
        compound.putInt("CooldownTicks", cooldownTicks);
        compound.putFloat("TotalHealth", totalHealth);
        compound.putInt("CX", center.getX());
        compound.putInt("CY", center.getY());
        compound.putInt("CZ", center.getZ());
        compound.putUUID("TargetPlayer", targetPlayerUUID);
        compound.putString("Status", status.name());
        return compound;
    }

    // Getters
    public boolean isActive() { return active; }
    public boolean isOver() { return status == RaidStatus.VICTORY || status == RaidStatus.DEFEAT; }
    public int getId() { return id; }
    public UUID getTargetPlayerUUID() { return targetPlayerUUID; }
    public RaidStatus getStatus() { return status; }
    public int getCurrentWave() { return currentWave; }

    public enum RaidStatus {
        ONGOING,
        VICTORY,
        DEFEAT
    }
}
