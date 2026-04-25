package net.alshanex.magic_realms.util.humans.mercenaries.chat;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Draws a small 8x8 face icon at a given chat-line screen position, using the speaker's current entity texture.
 */
@OnlyIn(Dist.CLIENT)
public final class ChatFaceRenderer {

    /** Pixel dimensions of the rendered icon in chat. Vanilla chat line height is 9, so 8 fits nicely. */
    public static final int ICON_SIZE = 8;

    /** Horizontal gap between the face and the start of the message text. */
    public static final int ICON_PADDING = 1;

    /** How long an entity-by-UUID lookup result stays cached, in real milliseconds. */
    private static final long CACHE_TTL_MS = 5_000L;

    private record CacheEntry(Entity entity, long expiresAt) {}

    private static final Map<UUID, CacheEntry> ENTITY_CACHE = new HashMap<>();

    private ChatFaceRenderer() {}

    /**
     * Total horizontal space the icon takes up (including padding). Used by the mixin when shifting message text right.
     */
    public static int totalWidth() {
        return ICON_SIZE + ICON_PADDING;
    }

    /**
     * Try to draw the face for the given UUID at (x, y). Returns true if anything was drawn (so the caller knows to shift text).
     */
    public static boolean draw(GuiGraphics g, UUID uuid, int x, int y) {
        if (uuid == null) return false;

        Entity entity = lookupEntity(uuid);
        if (!(entity instanceof IChatFaceProvider provider)) return false;

        ResourceLocation tex = provider.getChatFaceTextureCS();
        if (tex == null) return false;

        IChatFaceProvider.UVSlice face = provider.getChatFaceUV();
        IChatFaceProvider.UVSlice hat  = provider.getChatFaceHatUV();

        try {
            g.blit(tex, x, y, ICON_SIZE, ICON_SIZE, face.u(), face.v(), face.w(), face.h(), face.atlasW(), face.atlasH());
            if (hat != null) {
                g.blit(tex, x, y, ICON_SIZE, ICON_SIZE, hat.u(),  hat.v(),  hat.w(),  hat.h(),  hat.atlasW(),  hat.atlasH());
            }
            return true;
        } catch (Exception e) {
            // Don't let chat rendering crash the client over a missing texture.
            MagicRealms.LOGGER.debug("Failed to draw chat face for {}: {}", uuid, e.toString());
            return false;
        }
    }

    private static Entity lookupEntity(UUID uuid) {
        long now = System.currentTimeMillis();
        CacheEntry hit = ENTITY_CACHE.get(uuid);
        if (hit != null && hit.expiresAt > now && hit.entity != null && !hit.entity.isRemoved()) {
            return hit.entity;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return null;

        Entity found = null;
        // ClientLevel exposes entitiesForRendering(); UUID lookup isn't indexed on the client, so we walk and break on match.
        for (Entity e : level.entitiesForRendering()) {
            if (uuid.equals(e.getUUID())) {
                found = e;
                break;
            }
        }

        ENTITY_CACHE.put(uuid, new CacheEntry(found, now + CACHE_TTL_MS));

        // Periodic cleanup so the cache doesn't grow unbounded over a long session.
        if ((now & 0x3FFF) == 0) {
            ENTITY_CACHE.entrySet().removeIf(e -> e.getValue().expiresAt() <= now);
        }

        return found;
    }

    /** For when the player joins a new world / disconnects — the previous cache is now stale. */
    public static void clearCache() {
        ENTITY_CACHE.clear();
    }
}
