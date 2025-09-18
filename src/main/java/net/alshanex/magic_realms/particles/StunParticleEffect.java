package net.alshanex.magic_realms.particles;

import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;

public class StunParticleEffect {

    public static void spawnStunParticles(AbstractMercenaryEntity entity, ClientLevel level) {
        if (entity == null || !entity.isStunned()) {
            return;
        }

        double entityX = entity.getX();
        double entityY = entity.getY() + entity.getBbHeight() + 0.5; // Above the head
        double entityZ = entity.getZ();

        // Create spinning stars around the head
        long ticks = level.getGameTime();
        float time = ticks * 0.1f;

        for (int i = 0; i < 5; i++) {
            float angle = time + (i * 2.0944f); // 120 degrees apart (2π/3)
            float radius = 0.2f;

            double starX = entityX + Mth.cos(angle) * radius;
            double starZ = entityZ + Mth.sin(angle) * radius;

            // Spawn star particles
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    starX, entityY, starZ,
                    0.1, 0, 0.1);
        }
    }
}
