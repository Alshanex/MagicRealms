package net.alshanex.magic_realms.entity.random;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.alshanex.magic_realms.util.humans.TextureComponents;
import net.alshanex.magic_realms.util.humans.DynamicTextureManager;
import net.alshanex.magic_realms.util.ArmBackTextureFixer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RandomHumanEntityRenderer extends AbstractMercenaryEntityRenderer {
    private static final Map<String, ResourceLocation> COMPOSITE_TEXTURE_CACHE = new ConcurrentHashMap<>();

    // Fallback texture
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");

    public RandomHumanEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RandomHumanEntityModel());
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSpellCastingMob entity) {
        if (!(entity instanceof RandomHumanEntity human)) {
            MagicRealms.LOGGER.debug("Entity is not RandomHumanEntity, using default texture");
            return DEFAULT_TEXTURE;
        }

        TextureComponents components = human.getTextureComponents();

        // If no components on client side, entity hasn't generated textures yet
        if (components == null) {
            MagicRealms.LOGGER.debug("No texture components found for entity {}, using default texture",
                    human.getEntityName());
            return DEFAULT_TEXTURE;
        }

        // For preset textures, use directly
        if (components.isPresetTexture() && components.getSkinTexture() != null) {
            try {
                ResourceLocation presetTexture = ResourceLocation.parse(components.getSkinTexture());
                MagicRealms.LOGGER.debug("Using preset texture for {}: {}",
                        human.getEntityName(), presetTexture);
                return presetTexture;
            } catch (Exception e) {
                MagicRealms.LOGGER.warn("Invalid preset texture for {}: {}, falling back to default",
                        human.getEntityName(), components.getSkinTexture());
                return DEFAULT_TEXTURE;
            }
        }

        // For layered textures, create composite
        if (!components.isPresetTexture()) {
            String cacheKey = createCacheKey(components);
            if (cacheKey != null) {
                ResourceLocation compositeTexture = COMPOSITE_TEXTURE_CACHE.computeIfAbsent(
                        cacheKey, k -> createCompositeTexture(components));

                if (compositeTexture != null) {
                    return compositeTexture;
                } else {
                    MagicRealms.LOGGER.warn("Failed to create composite texture for {}", human.getEntityName());
                }
            } else {
                MagicRealms.LOGGER.warn("Could not create cache key for entity {}", human.getEntityName());
            }
        }

        MagicRealms.LOGGER.debug("Falling back to default texture for entity {}", human.getEntityName());
        return DEFAULT_TEXTURE;
    }

    private String createCacheKey(TextureComponents components) {
        try {
            return String.join("|",
                    components.getSkinTexture() != null ? components.getSkinTexture() : "",
                    components.getClothesTexture() != null ? components.getClothesTexture() : "",
                    components.getEyesTexture() != null ? components.getEyesTexture() : "",
                    components.getHairTexture() != null ? components.getHairTexture() : ""
            );
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to create cache key", e);
            return null;
        }
    }

    private ResourceLocation createCompositeTexture(TextureComponents components) {
        try {
            // Load base skin texture - this is required
            BufferedImage skinImage = loadTextureImage(components.getSkinTexture());
            if (skinImage == null) {
                MagicRealms.LOGGER.warn("Could not load skin texture: {}", components.getSkinTexture());
                return DEFAULT_TEXTURE;
            }

            // Create composite image
            int width = skinImage.getWidth();
            int height = skinImage.getHeight();
            BufferedImage composite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = composite.createGraphics();

            // Set up rendering
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.setComposite(AlphaComposite.SrcOver);

            // Clear with transparent
            g2d.setColor(new Color(0, 0, 0, 0));
            g2d.fillRect(0, 0, width, height);

            // Draw layers in order: skin -> clothes -> eyes -> hair
            g2d.drawImage(skinImage, 0, 0, null);

            if (components.getClothesTexture() != null) {
                BufferedImage clothesImage = loadTextureImage(components.getClothesTexture());
                if (clothesImage != null) {
                    g2d.drawImage(clothesImage, 0, 0, null);
                } else {
                    MagicRealms.LOGGER.debug("Could not load clothes texture: {}", components.getClothesTexture());
                }
            }

            if (components.getEyesTexture() != null) {
                BufferedImage eyesImage = loadTextureImage(components.getEyesTexture());
                if (eyesImage != null) {
                    g2d.drawImage(eyesImage, 0, 0, null);
                } else {
                    MagicRealms.LOGGER.debug("Could not load eyes texture: {}", components.getEyesTexture());
                }
            }

            if (components.getHairTexture() != null) {
                BufferedImage hairImage = loadTextureImage(components.getHairTexture());
                if (hairImage != null) {
                    g2d.drawImage(hairImage, 0, 0, null);
                } else {
                    MagicRealms.LOGGER.debug("Could not load hair texture: {}", components.getHairTexture());
                }
            }

            g2d.dispose();

            // Apply arm fix
            BufferedImage finalTexture = ArmBackTextureFixer.fixArmBackStripes(composite);
            if (finalTexture == null) {
                finalTexture = composite; // Use original if fix fails
            }

            // Register as dynamic texture
            String textureId = "composite_" + System.nanoTime() + "_" + Math.abs(createCacheKey(components).hashCode());
            ResourceLocation dynamicLocation = DynamicTextureManager.registerDynamicTexture(textureId, finalTexture);

            if (dynamicLocation != null) {
                MagicRealms.LOGGER.debug("Successfully created composite texture: {}", dynamicLocation);
                return dynamicLocation;
            } else {
                MagicRealms.LOGGER.warn("Failed to register dynamic texture");
                return DEFAULT_TEXTURE;
            }

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to create composite texture", e);
            return DEFAULT_TEXTURE;
        }
    }

    private BufferedImage loadTextureImage(String textureId) {
        if (textureId == null || textureId.isEmpty()) {
            return null;
        }

        try {
            ResourceLocation location = ResourceLocation.parse(textureId);
            Minecraft mc = Minecraft.getInstance();

            if (mc.getResourceManager() != null) {
                Resource resource = mc.getResourceManager().getResource(location).orElse(null);
                if (resource != null) {
                    try (InputStream inputStream = resource.open()) {
                        BufferedImage image = ImageIO.read(inputStream);
                        if (image != null) {
                            MagicRealms.LOGGER.debug("Successfully loaded texture: {}", textureId);
                        }
                        return image;
                    }
                } else {
                    MagicRealms.LOGGER.debug("Resource not found: {}", textureId);
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Failed to load texture: {} - {}", textureId, e.getMessage());
        }
        return null;
    }

    private ResourceLocation getDefaultTexture() {
        return DEFAULT_TEXTURE;
    }

    // Clean up cache when needed
    public static void clearCompositeCache() {
        COMPOSITE_TEXTURE_CACHE.clear();
        MagicRealms.LOGGER.debug("Cleared composite texture cache");
    }

    // Get cache statistics
    public static int getCacheSize() {
        return COMPOSITE_TEXTURE_CACHE.size();
    }
}