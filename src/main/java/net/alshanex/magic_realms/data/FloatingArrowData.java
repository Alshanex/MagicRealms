package net.alshanex.magic_realms.data;

import net.alshanex.magic_realms.entity.flying_arrow.FloatingArrowEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.UUID;

public class FloatingArrowData {

    @Nullable
    private UUID arrowId;

    /** Transient cache; never serialized. Cleared whenever the UUID is changed. */
    @Nullable
    private transient FloatingArrowEntity cached;

    public FloatingArrowData() {}

    public void setArrow(@Nullable FloatingArrowEntity arrow) {
        this.arrowId = arrow == null ? null : arrow.getUUID();
        this.cached = arrow;
    }

    public void clear() {
        this.arrowId = null;
        this.cached = null;
    }

    @Nullable
    public UUID getArrowId() {
        return arrowId;
    }

    /**
     * Resolves the live arrow entity, if any. Uses a cache and falls back to {@link ServerLevel#getEntity(UUID)} if the cache is stale (entity
     * removed, dimension change, etc.). Returns null if the arrow no longer exists, and clears the stored UUID in that case so we don't keep looking it up.
     */
    @Nullable
    public FloatingArrowEntity resolve(ServerLevel level) {
        if (arrowId == null) return null;

        // Fast path: cached reference is still valid and lives in this level.
        if (cached != null && !cached.isRemoved() && cached.level() == level) {
            return cached;
        }

        // Slow path: UUID lookup on the server level.
        Entity found = level.getEntity(arrowId);
        if (found instanceof FloatingArrowEntity arrow && !arrow.isRemoved()) {
            this.cached = arrow;
            return arrow;
        }

        // Stale UUID — entity is gone. Clear so we stop trying.
        clear();
        return null;
    }
}
