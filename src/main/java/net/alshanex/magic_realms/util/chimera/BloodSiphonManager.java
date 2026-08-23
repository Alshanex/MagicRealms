package net.alshanex.magic_realms.util.chimera;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages active blood siphon particle effects that run over multiple ticks.
 * <p>
 * Each siphon tracks a caster→target pair and spawns particles every tick until the duration expires. Automatically cleans up finished or invalid siphons.
 * <p>
 * Must be ticked every server tick via {@link #tick()}.
 */
public final class BloodSiphonManager {

    private BloodSiphonManager() {}

    private static final List<ActiveSiphon> activeSiphons = new ArrayList<>();

    /**
     * Start a new blood siphon effect from the caster to the target.
     */
    public static void startSiphon(ServerPlayer caster, Entity target) {
        activeSiphons.add(new ActiveSiphon(caster, target, 0));
    }

    /**
     * Call this every server tick to advance all active siphon effects.
     */
    public static void tick() {
        Iterator<ActiveSiphon> it = activeSiphons.iterator();
        while (it.hasNext()) {
            ActiveSiphon siphon = it.next();

            // Remove if entities are gone or effect is done
            if (siphon.caster.isRemoved() || siphon.target.isRemoved()
                    || siphon.ticksAlive >= BloodSiphonEffect.SIPHON_DURATION_TICKS
                    || !(siphon.caster.level() instanceof ServerLevel serverLevel)) {
                it.remove();
                continue;
            }

            BloodSiphonEffect.spawnSiphonTick(serverLevel, siphon.caster, siphon.target, siphon.ticksAlive);
            siphon.ticksAlive++;
        }
    }

    /**
     * Clear all active siphons (e.g., on server stop).
     */
    public static void clear() {
        activeSiphons.clear();
    }

    private static class ActiveSiphon {
        final ServerPlayer caster;
        final Entity target;
        int ticksAlive;

        ActiveSiphon(ServerPlayer caster, Entity target, int ticksAlive) {
            this.caster = caster;
            this.target = target;
            this.ticksAlive = ticksAlive;
        }
    }
}
