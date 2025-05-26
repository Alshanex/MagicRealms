package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.ArmBackTextureFixer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class CombinedTextureManager {
    private static final Map<String, ResourceLocation> COMBINED_TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static Path textureOutputDir;

    public static void initializeDirectories() {
        try {
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            textureOutputDir = gameDir.resolve("magic_realms_textures").resolve("entity").resolve("human");
            Files.createDirectories(textureOutputDir);
            MagicRealms.LOGGER.info("Created texture directory at: " + textureOutputDir.toString());
        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to create texture output directory", e);
        }
    }

    public static ResourceLocation getCombinedTextureWithHair(String entityUUID, Gender gender, EntityClass entityClass, int hairTextureIndex) {
        if (COMBINED_TEXTURE_CACHE.containsKey(entityUUID)) {
            MagicRealms.LOGGER.debug("Using cached complete texture for entity: " + entityUUID);
            return COMBINED_TEXTURE_CACHE.get(entityUUID);
        }

        // Verificar si existe en disco
        if (textureOutputDir != null) {
            Path texturePath = textureOutputDir.resolve(entityUUID + "_complete.png");
            if (Files.exists(texturePath)) {
                try {
                    BufferedImage existingImage = ImageIO.read(texturePath.toFile());
                    ResourceLocation location = DynamicTextureManager.registerDynamicTexture(entityUUID, existingImage);
                    if (location != null) {
                        COMBINED_TEXTURE_CACHE.put(entityUUID, location);
                        MagicRealms.LOGGER.debug("Loaded existing complete texture from disk for entity: " + entityUUID);
                        return location;
                    }
                } catch (IOException e) {
                    MagicRealms.LOGGER.error("Failed to load existing complete texture for entity " + entityUUID, e);
                }
            }
        }

        try {
            BufferedImage combinedImage = createCompleteTexture(gender, entityClass, hairTextureIndex);
            if (combinedImage != null) {
                ResourceLocation location = DynamicTextureManager.registerDynamicTexture(entityUUID, combinedImage);
                if (location != null) {
                    COMBINED_TEXTURE_CACHE.put(entityUUID, location);

                    // Guardar en disco
                    if (textureOutputDir != null) {
                        try {
                            Path texturePath = textureOutputDir.resolve(entityUUID + "_complete.png");
                            ImageIO.write(combinedImage, "PNG", texturePath.toFile());
                            MagicRealms.LOGGER.debug("Created and saved complete texture for entity: " + entityUUID);
                        } catch (IOException e) {
                            MagicRealms.LOGGER.error("Failed to save complete texture to disk for entity " + entityUUID, e);
                        }
                    }

                    return location;
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to create complete texture for entity " + entityUUID, e);
        }

        return null;
    }

    private static BufferedImage createCompleteTexture(Gender gender, EntityClass entityClass, int hairTextureIndex) {
        try {
            // Obtener texturas en el orden correcto
            ResourceLocation skinTexture = LayeredTextureManager.getRandomTexture("skin");
            ResourceLocation clothesTexture = getRandomClothesTexture(gender, entityClass);
            ResourceLocation eyesTexture = LayeredTextureManager.getRandomTexture("eyes");
            ResourceLocation hairTexture = getHairTexture(gender, hairTextureIndex);

            // Cargar la imagen base (skin)
            BufferedImage skinImage = loadImage(skinTexture);
            if (skinImage == null) {
                MagicRealms.LOGGER.error("Failed to load skin texture");
                return null;
            }

            // Crear la imagen combinada
            int width = skinImage.getWidth();
            int height = skinImage.getHeight();
            BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = combinedImage.createGraphics();

            // Configurar para preservar transparencias y calidad
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2d.setComposite(AlphaComposite.SrcOver);

            // Limpiar con transparente
            g2d.setColor(new Color(0, 0, 0, 0));
            g2d.fillRect(0, 0, width, height);

            // Dibujar capas en orden: skin -> clothes -> eyes -> hair

            // 1. Skin (base)
            g2d.drawImage(skinImage, 0, 0, null);
            MagicRealms.LOGGER.debug("Drew skin layer");

            // 2. Clothes
            if (clothesTexture != null) {
                BufferedImage clothesImage = loadImage(clothesTexture);
                if (clothesImage != null) {
                    g2d.drawImage(clothesImage, 0, 0, null);
                    MagicRealms.LOGGER.debug("Drew clothes layer");
                }
            }

            // 3. Eyes
            if (eyesTexture != null) {
                BufferedImage eyesImage = loadImage(eyesTexture);
                if (eyesImage != null) {
                    g2d.drawImage(eyesImage, 0, 0, null);
                    MagicRealms.LOGGER.debug("Drew eyes layer");
                }
            }

            // 4. Hair (último)
            if (hairTexture != null) {
                BufferedImage hairImage = loadImage(hairTexture);
                if (hairImage != null) {
                    g2d.drawImage(hairImage, 0, 0, null);
                    MagicRealms.LOGGER.debug("Drew hair layer");
                } else {
                    MagicRealms.LOGGER.debug("Hair texture not found or failed to load");
                }
            } else {
                MagicRealms.LOGGER.debug("No hair texture specified");
            }

            g2d.dispose();

            // Aplicar el fix de brazos
            BufferedImage finalTexture = ArmBackTextureFixer.fixArmBackStripes(combinedImage);
            MagicRealms.LOGGER.debug("Successfully created complete texture with all layers including hair");

            return finalTexture;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error creating complete texture", e);
            return null;
        }
    }

    private static ResourceLocation getHairTexture(Gender gender, int hairTextureIndex) {
        if (hairTextureIndex < 0) {
            MagicRealms.LOGGER.debug("Hair texture index is negative, no hair will be added");
            return null;
        }

        String category = "hair_" + gender.getName();
        ResourceLocation hairTexture = LayeredTextureManager.getTextureByIndex(category, hairTextureIndex);

        if (hairTexture != null) {
            MagicRealms.LOGGER.debug("Using hair texture: {} (index: {})", hairTexture, hairTextureIndex);
        } else {
            MagicRealms.LOGGER.warn("Failed to get hair texture for category: {} with index: {}", category, hairTextureIndex);
        }

        return hairTexture;
    }

    private static BufferedImage loadImage(ResourceLocation location) {
        if (location == null) return null;

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getResourceManager() != null) {
                Resource resource = mc.getResourceManager().getResource(location).orElse(null);
                if (resource != null) {
                    try (InputStream inputStream = resource.open()) {
                        BufferedImage image = ImageIO.read(inputStream);
                        if (image != null) {
                            MagicRealms.LOGGER.debug("Successfully loaded image: " + location);
                            return image;
                        }
                    }
                }
            }
        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to load image: " + location, e);
        }
        return null;
    }

    private static ResourceLocation getRandomClothesTexture(Gender gender, EntityClass entityClass) {
        java.util.List<ResourceLocation> availableTextures = new java.util.ArrayList<>();

        java.util.List<ResourceLocation> classTextures = LayeredTextureManager.TEXTURE_CACHE.get("clothes_" + entityClass.getName() + "_" + gender.getName());
        if (classTextures != null && !classTextures.isEmpty()) {
            availableTextures.addAll(classTextures);
        }

        if (availableTextures.isEmpty()) {
            java.util.List<ResourceLocation> commonTextures = LayeredTextureManager.TEXTURE_CACHE.get("clothes_common_" + gender.getName());
            if (commonTextures != null) {
                availableTextures.addAll(commonTextures);
            }
        }

        if (availableTextures.isEmpty()) {
            return null;
        }

        return availableTextures.get(new java.util.Random().nextInt(availableTextures.size()));
    }

    public static void removeEntityTexture(String entityUUID) {
        try {
            // Remover la textura completa
            COMBINED_TEXTURE_CACHE.remove(entityUUID);
            DynamicTextureManager.unregisterTexture(entityUUID);

            if (textureOutputDir != null) {
                Path completeTexturePath = textureOutputDir.resolve(entityUUID + "_complete.png");
                if (Files.exists(completeTexturePath)) {
                    Files.delete(completeTexturePath);
                    MagicRealms.LOGGER.debug("Deleted complete texture file for entity: " + entityUUID);
                }
            }
        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to delete complete texture for entity " + entityUUID, e);
        }
    }

    public static void clearCache() {
        COMBINED_TEXTURE_CACHE.clear();
        DynamicTextureManager.clearAllTextures();
    }
}
