package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
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
            // Crear el directorio de salida en el directorio del juego
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            textureOutputDir = gameDir.resolve("magic_realms_textures").resolve("entity").resolve("human");
            Files.createDirectories(textureOutputDir);
            MagicRealms.LOGGER.info("Created texture directory at: " + textureOutputDir.toString());
        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to create texture output directory", e);
        }
    }

    public static ResourceLocation getCombinedTexture(String entityUUID, Gender gender, EntityClass entityClass) {
        // Verificar si ya tenemos la textura registrada dinámicamente
        if (DynamicTextureManager.hasTexture(entityUUID)) {
            return COMBINED_TEXTURE_CACHE.get(entityUUID);
        }

        // Verificar si el archivo ya existe en disco
        if (textureOutputDir != null) {
            Path texturePath = textureOutputDir.resolve(entityUUID + ".png");
            if (Files.exists(texturePath)) {
                try {
                    BufferedImage existingImage = ImageIO.read(texturePath.toFile());
                    ResourceLocation location = DynamicTextureManager.registerDynamicTexture(entityUUID, existingImage);
                    if (location != null) {
                        COMBINED_TEXTURE_CACHE.put(entityUUID, location);
                        return location;
                    }
                } catch (IOException e) {
                    MagicRealms.LOGGER.error("Failed to load existing texture for entity " + entityUUID, e);
                }
            }
        }

        // Crear la textura combinada
        try {
            BufferedImage combinedImage = createCombinedTexture(gender, entityClass);
            if (combinedImage != null) {
                // Registrar la textura dinámicamente
                ResourceLocation location = DynamicTextureManager.registerDynamicTexture(entityUUID, combinedImage);
                if (location != null) {
                    COMBINED_TEXTURE_CACHE.put(entityUUID, location);

                    // Opcionalmente, guardar en disco para cache
                    if (textureOutputDir != null) {
                        try {
                            Path texturePath = textureOutputDir.resolve(entityUUID + ".png");
                            ImageIO.write(combinedImage, "PNG", texturePath.toFile());
                        } catch (IOException e) {
                            MagicRealms.LOGGER.error("Failed to save texture to disk for entity " + entityUUID, e);
                        }
                    }

                    return location;
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to create combined texture for entity " + entityUUID, e);
        }

        // Si falla, devolver null
        return null;
    }

    private static BufferedImage createCombinedTexture(Gender gender, EntityClass entityClass) {
        try {
            // Obtener las texturas aleatorias
            ResourceLocation skinTexture = LayeredTextureManager.getRandomTexture("skin");
            ResourceLocation clothesTexture = getRandomClothesTexture(gender, entityClass);
            ResourceLocation eyesTexture = LayeredTextureManager.getRandomTexture("eyes");
            ResourceLocation hairTexture = LayeredTextureManager.getRandomTexture("hair_" + gender.getName());

            // Cargar las imágenes
            BufferedImage skinImage = loadImage(skinTexture);
            if (skinImage == null) {
                MagicRealms.LOGGER.error("Failed to load skin texture");
                return null;
            }

            // Crear la imagen base con las dimensiones de la skin
            int width = skinImage.getWidth();
            int height = skinImage.getHeight();
            BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = combinedImage.createGraphics();

            // Configurar para mejor calidad
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

            // Dibujar las capas en orden: skin -> clothes -> eyes -> hair
            g2d.drawImage(skinImage, 0, 0, null);

            if (clothesTexture != null) {
                BufferedImage clothesImage = loadImage(clothesTexture);
                if (clothesImage != null) {
                    g2d.drawImage(clothesImage, 0, 0, null);
                }
            }

            if (eyesTexture != null) {
                BufferedImage eyesImage = loadImage(eyesTexture);
                if (eyesImage != null) {
                    g2d.drawImage(eyesImage, 0, 0, null);
                }
            }

            if (hairTexture != null) {
                BufferedImage hairImage = loadImage(hairTexture);
                if (hairImage != null) {
                    g2d.drawImage(hairImage, 0, 0, null);
                }
            }

            g2d.dispose();
            return combinedImage;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error creating combined texture", e);
            return null;
        }
    }

    private static BufferedImage loadImage(ResourceLocation location) {
        if (location == null) return null;

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getResourceManager() != null) {
                Resource resource = mc.getResourceManager().getResource(location).orElse(null);
                if (resource != null) {
                    try (InputStream inputStream = resource.open()) {
                        return ImageIO.read(inputStream);
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

        java.util.List<ResourceLocation> commonTextures = LayeredTextureManager.TEXTURE_CACHE.get("clothes_common_" + gender.getName());
        if (commonTextures != null) {
            availableTextures.addAll(commonTextures);
        }

        java.util.List<ResourceLocation> classTextures = LayeredTextureManager.TEXTURE_CACHE.get("clothes_" + entityClass.getName() + "_" + gender.getName());
        if (classTextures != null) {
            availableTextures.addAll(classTextures);
        }

        if (availableTextures.isEmpty()) {
            return null;
        }

        return availableTextures.get(new java.util.Random().nextInt(availableTextures.size()));
    }

    public static void removeEntityTexture(String entityUUID) {
        try {
            // Eliminar del caché local
            COMBINED_TEXTURE_CACHE.remove(entityUUID);

            // Desregistrar la textura dinámica
            DynamicTextureManager.unregisterTexture(entityUUID);

            // Eliminar el archivo del disco si existe
            if (textureOutputDir != null) {
                Path texturePath = textureOutputDir.resolve(entityUUID + ".png");
                if (Files.exists(texturePath)) {
                    Files.delete(texturePath);
                    MagicRealms.LOGGER.debug("Deleted texture file for entity: " + entityUUID);
                }
            }
        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to delete texture for entity " + entityUUID, e);
        }
    }

    public static void clearCache() {
        COMBINED_TEXTURE_CACHE.clear();
        DynamicTextureManager.clearAllTextures();
    }

    public static void cleanupOldTextures() {
        try {
            if (textureOutputDir != null && Files.exists(textureOutputDir)) {
                Files.walk(textureOutputDir)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".png"))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                MagicRealms.LOGGER.error("Failed to delete old texture: " + path, e);
                            }
                        });
                MagicRealms.LOGGER.info("Cleaned up old texture files");
            }
        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to cleanup old textures", e);
        }
    }
}
