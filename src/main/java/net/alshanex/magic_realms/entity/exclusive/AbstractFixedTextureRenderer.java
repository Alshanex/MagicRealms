package net.alshanex.magic_realms.entity.exclusive;

import com.mojang.blaze3d.platform.NativeImage;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntityModel;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.alshanex.magic_realms.entity.exclusive.lilac.LilacEntityRenderer;
import net.alshanex.magic_realms.skins_management.ArmBackTextureFixer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractFixedTextureRenderer extends AbstractMercenaryEntityRenderer {
    // Static cache shared among all renderer instances
    private static final Map<ResourceLocation, ResourceLocation> PROCESSED_TEXTURES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, BufferedImage> PROCESSED_IMAGES = new ConcurrentHashMap<>();

    public AbstractFixedTextureRenderer(EntityRendererProvider.Context renderManager, AbstractMercenaryEntityModel model) {
        super(renderManager, model);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSpellCastingMob entity) {
        ResourceLocation originalTexture = getOriginalTexture();

        // Safety check
        if (originalTexture == null) {
            MagicRealms.LOGGER.warn("Original texture is null in renderer: {}", this.getClass().getSimpleName());
            return null;
        }

        return getProcessedTexture(originalTexture);
    }

    /**
     * Override this method to return the original texture location for this entity
     */
    protected abstract ResourceLocation getOriginalTexture();

    /**
     * Gets or creates a processed texture with arm fixes applied
     */
    private ResourceLocation getProcessedTexture(ResourceLocation originalTexture) {
        // Check if we have a valid processed texture that's still registered
        ResourceLocation cached = PROCESSED_TEXTURES.get(originalTexture);
        if (cached != null && isTextureValid(cached)) {
            return cached;
        }

        // Try to re-register from cached processed image
        BufferedImage cachedImage = PROCESSED_IMAGES.get(originalTexture);
        if (cachedImage != null) {
            ResourceLocation reregistered = registerProcessedTexture(originalTexture, cachedImage);
            if (reregistered != null) {
                PROCESSED_TEXTURES.put(originalTexture, reregistered);
                //MagicRealms.LOGGER.debug("Re-registered cached processed texture: {}", originalTexture);
                return reregistered;
            }
        }

        // Process texture from scratch
        return processNewTexture(originalTexture);
    }

    /**
     * Processes a texture from the original file
     */
    private ResourceLocation processNewTexture(ResourceLocation originalTexture) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getResourceManager() == null) {
                MagicRealms.LOGGER.warn("Minecraft instance or ResourceManager is null");
                return originalTexture;
            }

            Resource resource = mc.getResourceManager().getResource(originalTexture).orElse(null);

            if (resource != null) {
                try (InputStream inputStream = resource.open()) {
                    BufferedImage originalImage = ImageIO.read(inputStream);

                    if (originalImage != null) {
                        BufferedImage fixedImage = null;

                        if(shouldFixArms()){
                            fixedImage = ArmBackTextureFixer.fixArmBackStripes(originalImage);
                        } else {
                            fixedImage = originalImage;
                        }

                        if (fixedImage != null) {
                            // Cache the processed image for potential re-registration
                            PROCESSED_IMAGES.put(originalTexture, fixedImage);

                            // Register the texture
                            ResourceLocation processed = registerProcessedTexture(originalTexture, fixedImage);
                            if (processed != null) {
                                PROCESSED_TEXTURES.put(originalTexture, processed);
                                //MagicRealms.LOGGER.debug("Successfully processed new texture: {}", originalTexture);
                                return processed;
                            }
                        } else {
                            MagicRealms.LOGGER.debug("ArmBackTextureFixer returned null for: {}", originalTexture);
                        }
                    } else {
                        MagicRealms.LOGGER.warn("Could not read image data from: {}", originalTexture);
                    }
                }
            } else {
                MagicRealms.LOGGER.warn("Could not find resource: {}", originalTexture);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to process texture: " + originalTexture, e);
        }

        // Return original texture if processing fails
        return originalTexture;
    }

    /**
     * Registers a processed BufferedImage as a DynamicTexture with Minecraft's TextureManager
     */
    private ResourceLocation registerProcessedTexture(ResourceLocation originalTexture, BufferedImage image) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getTextureManager() == null) {
                MagicRealms.LOGGER.warn("Cannot register texture - TextureManager is null");
                return null;
            }

            // Convert BufferedImage to NativeImage
            NativeImage nativeImage = convertToNativeImage(image);
            if (nativeImage == null) {
                MagicRealms.LOGGER.error("Failed to convert BufferedImage to NativeImage for: {}", originalTexture);
                return null;
            }

            // Create DynamicTexture
            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);

            // Create unique processed texture location
            String processedName = originalTexture.getPath().replace(".png", "_arm_fixed.png");
            ResourceLocation processedTexture = ResourceLocation.fromNamespaceAndPath(
                    originalTexture.getNamespace(), processedName);

            // Register with TextureManager
            mc.getTextureManager().register(processedTexture, dynamicTexture);

            //MagicRealms.LOGGER.debug("Registered processed texture: {} -> {}", originalTexture, processedTexture);
            return processedTexture;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to register processed texture for: " + originalTexture, e);
            return null;
        }
    }

    /**
     * Checks if a texture is still valid in the TextureManager
     */
    private boolean isTextureValid(ResourceLocation texture) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getTextureManager() == null) {
                return false;
            }

            // Try to get the texture - if it throws an exception or returns null, it's not valid
            return mc.getTextureManager().getTexture(texture) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Converts a BufferedImage to NativeImage for use with DynamicTexture
     */
    private NativeImage convertToNativeImage(BufferedImage bufferedImage) {
        try {
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();

            // Create NativeImage with RGBA format
            NativeImage nativeImage = new NativeImage(width, height, false);

            // Copy pixels from BufferedImage to NativeImage
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = bufferedImage.getRGB(x, y);

                    // Extract ARGB components from BufferedImage
                    int alpha = (rgb >> 24) & 0xFF;
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;

                    // NativeImage expects ABGR format (different byte order)
                    int abgr = (alpha << 24) | (blue << 16) | (green << 8) | red;

                    nativeImage.setPixelRGBA(x, y, abgr);
                }
            }

            return nativeImage;
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error converting BufferedImage to NativeImage", e);
            return null;
        }
    }

    // Static utility methods for cache management

    /**
     * Clears the texture registration cache (but keeps processed images for re-registration)
     * Call this when switching worlds or when texture manager is cleared
     */
    public static void clearTextureCache() {
        MagicRealms.LOGGER.debug("Clearing processed texture cache");
        PROCESSED_TEXTURES.clear();
        // Keep PROCESSED_IMAGES for potential re-registration
    }

    /**
     * Completely clears all caches including processed images
     * Use when you want to force complete reprocessing (e.g., resource pack change)
     */
    public static void clearAllCaches() {
        MagicRealms.LOGGER.debug("Clearing all texture processing caches");
        PROCESSED_TEXTURES.clear();
        PROCESSED_IMAGES.clear();
    }

    /**
     * Checks if a texture has been processed before (image exists in cache)
     */
    public static boolean hasProcessedImage(ResourceLocation texture) {
        return PROCESSED_IMAGES.containsKey(texture);
    }

    /**
     * Checks if a texture is currently registered and valid
     */
    public static boolean isTextureCurrentlyValid(ResourceLocation texture) {
        ResourceLocation processed = PROCESSED_TEXTURES.get(texture);
        return processed != null && isTextureValidStatic(processed);
    }

    private static boolean isTextureValidStatic(ResourceLocation texture) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc != null && mc.getTextureManager() != null &&
                    mc.getTextureManager().getTexture(texture) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean shouldFixArms(){
        return !(this instanceof LilacEntityRenderer);
    }

    /**
     * Gets cache statistics for debugging
     */
    public static String getCacheStats() {
        return String.format("Processed textures: %d registered, %d images cached",
                PROCESSED_TEXTURES.size(), PROCESSED_IMAGES.size());
    }
}
