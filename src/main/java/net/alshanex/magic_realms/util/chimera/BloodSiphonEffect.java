package net.alshanex.magic_realms.util.chimera;

import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Spawns a blood siphon particle stream from a caster to one or more target entities.
 * Uses Iron's Spells blood particles for the visual effect.
 * <p>
 * The siphon is a curved arc of particles that flows from the player's chest toward the target entity over multiple ticks.
 */
public final class BloodSiphonEffect {

    private BloodSiphonEffect() {}

    /**
     * Total duration in ticks for the siphon effect.
     */
    public static final int SIPHON_DURATION_TICKS = 30; // 1.5 seconds

    /**
     * Number of particles per tick along the stream.
     */
    private static final int PARTICLES_PER_TICK = 6;

    /**
     * Spawn a single tick's worth of blood siphon particles from the caster to the target.
     */
    public static void spawnSiphonTick(ServerLevel level, ServerPlayer caster, Entity target, int ticksAlive) {
        float progress = (float) ticksAlive / SIPHON_DURATION_TICKS;

        // Source: player's chest area
        Vec3 start = caster.position().add(0, caster.getBbHeight() * 0.6, 0);
        // Destination: target's center
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.5, 0);

        Vec3 direction = end.subtract(start);
        double distance = direction.length();

        // The "head" of the siphon advances over time
        // Particles trail behind the leading edge
        float leadProgress = Math.min(1.0f, progress * 1.4f); // head moves slightly faster than duration
        float trailLength = 0.4f; // how much of the path is filled with trailing particles

        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            // Distribute particles between the trail start and the lead
            float t = leadProgress - (trailLength * (1.0f - (float) i / PARTICLES_PER_TICK));
            t = Math.max(0.0f, Math.min(1.0f, t));

            // Interpolate position along the path with a slight upward arc
            double px = start.x + direction.x * t;
            double pz = start.z + direction.z * t;

            // Arc: parabolic curve that peaks at midpoint
            double arcHeight = distance * 0.15 * 4.0 * t * (1.0 - t);
            double py = start.y + direction.y * t + arcHeight;

            // Small random spread for a "stream" look
            double spread = 0.08;
            double ox = (level.random.nextDouble() - 0.5) * spread;
            double oy = (level.random.nextDouble() - 0.5) * spread;
            double oz = (level.random.nextDouble() - 0.5) * spread;

            // Small velocity toward the target for a flowing feel
            double speed = 0.02;
            double vx = direction.normalize().x * speed;
            double vy = direction.normalize().y * speed;
            double vz = direction.normalize().z * speed;

            // Blood particles
            level.sendParticles(
                    ParticleRegistry.BLOOD_PARTICLE.get(),
                    px + ox, py + oy, pz + oz,
                    1,     // count
                    vx, vy, vz,
                    0.01   // speed multiplier
            );
        }

        // Extra burst at the target when the siphon "arrives"
        if (progress > 0.7f && ticksAlive % 2 == 0) {
            level.sendParticles(
                    ParticleRegistry.BLOOD_PARTICLE.get(),
                    end.x, end.y, end.z,
                    3,
                    0.2, 0.3, 0.2,
                    0.02
            );
        }

        // Dripping particles at the source (player)
        if (ticksAlive % 3 == 0) {
            level.sendParticles(
                    ParticleRegistry.BLOOD_PARTICLE.get(),
                    start.x, start.y, start.z,
                    2,
                    0.1, 0.1, 0.1,
                    0.01
            );
        }
    }
}
