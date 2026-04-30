package net.alshanex.magic_realms.util.humans.mercenaries.chat;

import com.mojang.blaze3d.platform.NativeImage;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.TextureComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ChatFaceCompositeCache {

    /** Width of the mini atlas: 8 pixels of face + 8 pixels of hat overlay. */
    public static final int ATLAS_WIDTH = 16;
    /** Height of the mini atlas: 8 pixels (face/hat region height). */
    public static final int ATLAS_HEIGHT = 8;

    /** UV offset of the face slice within the mini atlas. */
    public static final int FACE_X = 0;
    public static final int HAT_X = 8;
    public static final int SLICE_Y = 0;
    public static final int SLICE_SIZE = 8;

    /** Source UV of the face region within a standard 64x64 player skin. */
    private static final int SRC_FACE_X = 8;
    private static final int SRC_FACE_Y = 8;
    /** Source UV of the hat region within a standard 64x64 player skin. */
    private static final int SRC_HAT_X = 40;
    private static final int SRC_HAT_Y = 8;

    private static final int MAX_CACHE_SIZE = 256;

    private static final LinkedHashMap<String, ResourceLocation> CACHE =
            new LinkedHashMap<>(8, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, ResourceLocation> eldest) {
                    if (size() > MAX_CACHE_SIZE) {
                        unregisterTexture(eldest.getValue());
                        return true;
                    }
                    return false;
                }
            };

    private ChatFaceCompositeCache() {}

    public static ResourceLocation getOrBuild(TextureComponents components) {
        if (components == null) return null;

        if (components.isPresetTexture()) return null;

        String key = createCacheKey(components);
        if (key == null) return null;

        synchronized (CACHE) {
            ResourceLocation existing = CACHE.get(key); // bumps LRU recency
            if (existing != null) return existing;

            ResourceLocation built = build(components, key);
            if (built != null) {
                CACHE.put(key, built);
            }
            return built;
        }
    }

    public static void clear() {
        synchronized (CACHE) {
            for (ResourceLocation loc : CACHE.values()) {
                unregisterTexture(loc);
            }
            CACHE.clear();
        }
    }

    public static void clearFor(TextureComponents components) {
        if (components == null || components.isPresetTexture()) return;
        String key = createCacheKey(components);
        if (key == null) return;
        synchronized (CACHE) {
            ResourceLocation removed = CACHE.remove(key);
            if (removed != null) {
                unregisterTexture(removed);
            }
        }
    }

    public static int size() {
        synchronized (CACHE) {
            return CACHE.size();
        }
    }

    private static ResourceLocation build(TextureComponents components, String cacheKey) {
        try {
            // Allocate the mini atlas. NativeImage is RGBA off-heap; freed when the DynamicTexture wrapping it is closed.
            NativeImage atlas = new NativeImage(NativeImage.Format.RGBA, ATLAS_WIDTH, ATLAS_HEIGHT, false);
            // Initialize fully transparent. NativeImage.fillRect handles this.
            atlas.fillRect(0, 0, ATLAS_WIDTH, ATLAS_HEIGHT, 0);

            // Layer order: skin -> clothes -> eyes -> hair.
            blendLayer(atlas, components.getSkinTexture(), true);
            blendLayer(atlas, components.getClothesTexture(), false);
            blendLayer(atlas, components.getEyesTexture(), false);
            blendLayer(atlas, components.getHairTexture(), false);

            // Wrap in a DynamicTexture and register.
            DynamicTexture tex = new DynamicTexture(atlas);

            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                    MagicRealms.MODID,
                    "chat_face/" + Math.abs(cacheKey.hashCode()));

            Minecraft.getInstance().getTextureManager().register(loc, tex);
            return loc;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to build chat face composite", e);
            return null;
        }
    }

    private static void blendLayer(NativeImage atlas, String textureId, boolean required) {
        if (textureId == null || textureId.isEmpty()) {
            if (required) {
                MagicRealms.LOGGER.debug("Chat face composite: required base layer is null");
            }
            return;
        }

        ResourceLocation assetPath;
        try {
            ResourceLocation parsed = ResourceLocation.parse(textureId);
            assetPath = ResourceLocation.fromNamespaceAndPath(
                    parsed.getNamespace(), "textures/" + parsed.getPath() + ".png");
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Chat face composite: invalid texture id '{}'", textureId);
            return;
        }

        Resource resource = Minecraft.getInstance().getResourceManager().getResource(assetPath).orElse(null);
        if (resource == null) {
            if (required) {
                MagicRealms.LOGGER.debug("Chat face composite: resource not found '{}'", assetPath);
            }
            return;
        }

        try (InputStream is = resource.open();
             NativeImage src = NativeImage.read(is)) {

            // Copy face region: src[8..15, 8..15] -> atlas[0..7, 0..7]
            blitRegion(src, SRC_FACE_X, SRC_FACE_Y, atlas, FACE_X, SLICE_Y, SLICE_SIZE);
            // Copy hat region: src[40..47, 8..15] -> atlas[8..15, 0..7]
            blitRegion(src, SRC_HAT_X, SRC_HAT_Y, atlas, HAT_X, SLICE_Y, SLICE_SIZE);

        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Chat face composite: failed to read '{}': {}",
                    assetPath, e.getMessage());
        }
    }

    private static void blitRegion(NativeImage src, int srcX, int srcY,
                                   NativeImage dst, int dstX, int dstY, int sliceSize) {
        for (int dy = 0; dy < sliceSize; dy++) {
            for (int dx = 0; dx < sliceSize; dx++) {
                int sx = srcX + dx;
                int sy = srcY + dy;

                if (sx < 0 || sy < 0 || sx >= src.getWidth() || sy >= src.getHeight()) continue;

                int srcPixel = src.getPixelRGBA(sx, sy);
                int srcA = (srcPixel >> 24) & 0xFF;
                if (srcA == 0) continue; // fully transparent: nothing to blend

                int outX = dstX + dx;
                int outY = dstY + dy;

                if (srcA == 255) {
                    // Fully opaque: just write.
                    dst.setPixelRGBA(outX, outY, srcPixel);
                } else {
                    // Partial alpha: blend over existing dst pixel.
                    int dstPixel = dst.getPixelRGBA(outX, outY);
                    dst.setPixelRGBA(outX, outY, alphaBlend(srcPixel, dstPixel));
                }
            }
        }
    }

    private static int alphaBlend(int src, int dst) {
        int sa = (src >> 24) & 0xFF;
        int sb = (src >> 16) & 0xFF;
        int sg = (src >> 8)  & 0xFF;
        int sr =  src        & 0xFF;

        int da = (dst >> 24) & 0xFF;
        int db = (dst >> 16) & 0xFF;
        int dg = (dst >> 8)  & 0xFF;
        int dr =  dst        & 0xFF;

        // Standard Porter-Duff "over": result = src + dst * (1 - srcAlpha)
        int oneMinusSa = 255 - sa;
        int outA = sa + (da * oneMinusSa + 127) / 255;
        int outR = sr + (dr * oneMinusSa + 127) / 255;
        int outG = sg + (dg * oneMinusSa + 127) / 255;
        int outB = sb + (db * oneMinusSa + 127) / 255;

        return (clamp255(outA) << 24)
                | (clamp255(outB) << 16)
                | (clamp255(outG) << 8)
                |  clamp255(outR);
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private static String createCacheKey(TextureComponents c) {
        try {
            return String.join("|",
                    c.getSkinTexture()    != null ? c.getSkinTexture()    : "",
                    c.getClothesTexture() != null ? c.getClothesTexture() : "",
                    c.getEyesTexture()    != null ? c.getEyesTexture()    : "",
                    c.getHairTexture()    != null ? c.getHairTexture()    : "");
        } catch (Exception e) {
            return null;
        }
    }

    private static void unregisterTexture(ResourceLocation loc) {
        try {
            Minecraft.getInstance().getTextureManager().release(loc);
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Failed to release chat face texture {}: {}", loc, e.getMessage());
        }
    }
}
