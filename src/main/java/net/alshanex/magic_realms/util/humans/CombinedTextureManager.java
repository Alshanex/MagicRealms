package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.ArmBackTextureFixer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
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
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class CombinedTextureManager {
    private static final Map<String, ResourceLocation> COMBINED_TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> ACTIVE_ENTITIES = ConcurrentHashMap.newKeySet();
    // New cache to store texture names for entities that used preset textures
    private static final Map<String, String> ENTITY_TEXTURE_NAMES = new ConcurrentHashMap<>();
    private static Path textureOutputDir;
    private static String currentWorldName = null;

    // Random instance for texture selection - can be made configurable later
    private static final Random TEXTURE_RANDOM = new Random();
    // Chance to use additional texture (30% = 0.3)
    private static final double ADDITIONAL_TEXTURE_CHANCE = 0.3;

    // Result class to hold both texture and name information
    public static class TextureResult {
        private final ResourceLocation textureLocation;
        private final String textureName;
        private final boolean isPresetTexture;

        public TextureResult(ResourceLocation textureLocation, String textureName, boolean isPresetTexture) {
            this.textureLocation = textureLocation;
            this.textureName = textureName;
            this.isPresetTexture = isPresetTexture;
        }

        public ResourceLocation getTextureLocation() { return textureLocation; }
        public String getTextureName() { return textureName; }
        public boolean isPresetTexture() { return isPresetTexture; }
    }

    // This will be called from WorldEventHandler when a world loads
    public static void setWorldDirectory(Path worldSaveDir, String worldName) {
        try {
            textureOutputDir = worldSaveDir.resolve("magic_realms_textures").resolve("entity").resolve("human");
            Files.createDirectories(textureOutputDir);
            MagicRealms.LOGGER.debug("Set texture directory for world '{}' at: {}", worldName, textureOutputDir.toString());

            // Check if we switched worlds
            if (!worldName.equals(currentWorldName)) {
                if (currentWorldName != null) {
                    // Clear cache when switching worlds, but don't delete files
                    clearCache();
                    MagicRealms.LOGGER.debug("Cleared texture cache due to world change: {} -> {}", currentWorldName, worldName);
                }
                currentWorldName = worldName;

                // Only clean up orphaned textures when first joining this world (not on every rejoin)
                // Check if this is a fresh session by looking for a session marker
                Path sessionMarker = textureOutputDir.resolve(".session_" + System.currentTimeMillis());
                try {
                    // Clean up old session markers (older than 1 minute - indicates previous session)
                    Files.walk(textureOutputDir)
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().startsWith(".session_"))
                            .filter(path -> {
                                try {
                                    long timestamp = Long.parseLong(path.getFileName().toString().substring(9));
                                    return System.currentTimeMillis() - timestamp > 60000; // 1 minute
                                } catch (NumberFormatException e) {
                                    return true; // Remove invalid session markers
                                }
                            })
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (Exception e) {
                                    // Ignore cleanup failures
                                }
                            });

                    // Create new session marker
                    Files.createFile(sessionMarker);
                    MagicRealms.LOGGER.debug("Created session marker for texture cleanup tracking");
                } catch (Exception e) {
                    MagicRealms.LOGGER.debug("Failed to manage session markers: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to create texture output directory for world '{}'", worldName, e);
        }
    }

    // Modified method that now returns TextureResult with name information
    public static TextureResult getCombinedTextureWithHairAndName(String entityUUID, Gender gender, EntityClass entityClass, int hairTextureIndex) {
        // SAFETY CHECK: Ensure we're using the correct world directory
        if (textureOutputDir == null) {
            MagicRealms.LOGGER.warn("Texture directory not set, attempting to initialize...");
            initializeDirectories();

            if (textureOutputDir == null) {
                MagicRealms.LOGGER.error("Failed to initialize texture directory!");
                return null;
            }
        }

        // ADDITIONAL SAFETY: Verify we're using the correct world directory, not the fallback
        try {
            Minecraft mc = Minecraft.getInstance();
            IntegratedServer server = mc.getSingleplayerServer();

            if (server != null) {
                // Check if current directory matches the actual world directory
                Path actualWorldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .resolve("magic_realms_textures").resolve("entity").resolve("human");

                if (!textureOutputDir.equals(actualWorldDir)) {
                    MagicRealms.LOGGER.warn("Texture directory mismatch detected!");
                    MagicRealms.LOGGER.warn("Current: {}", textureOutputDir);
                    MagicRealms.LOGGER.warn("Expected: {}", actualWorldDir);
                    MagicRealms.LOGGER.warn("Correcting texture directory...");

                    // Update to correct directory
                    textureOutputDir = actualWorldDir;
                    Files.createDirectories(textureOutputDir);
                    currentWorldName = server.getWorldData().getLevelName();

                    MagicRealms.LOGGER.debug("Corrected texture directory to: {}", textureOutputDir);
                }
            } else if (mc.getCurrentServer() != null) {
                // Similar check for multiplayer
                String serverName = mc.getCurrentServer().name.replaceAll("[^a-zA-Z0-9._-]", "_");
                Path expectedMultiplayerDir = mc.gameDirectory.toPath()
                        .resolve("multiplayer_textures").resolve(serverName)
                        .resolve("magic_realms_textures").resolve("entity").resolve("human");

                if (!textureOutputDir.equals(expectedMultiplayerDir)) {
                    MagicRealms.LOGGER.warn("Multiplayer texture directory mismatch detected!");
                    MagicRealms.LOGGER.warn("Current: {}", textureOutputDir);
                    MagicRealms.LOGGER.warn("Expected: {}", expectedMultiplayerDir);

                    textureOutputDir = expectedMultiplayerDir;
                    Files.createDirectories(textureOutputDir);
                    currentWorldName = "multiplayer_" + serverName;

                    MagicRealms.LOGGER.debug("Corrected multiplayer texture directory to: {}", textureOutputDir);
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error during texture directory validation: {}", e.getMessage());
        }

        MagicRealms.LOGGER.debug("Using texture directory: {}", textureOutputDir);

        // Track this entity as active
        ACTIVE_ENTITIES.add(entityUUID);

        if (COMBINED_TEXTURE_CACHE.containsKey(entityUUID)) {
            MagicRealms.LOGGER.debug("Using cached complete texture for entity: " + entityUUID);
            String cachedTextureName = ENTITY_TEXTURE_NAMES.get(entityUUID);
            boolean isPreset = cachedTextureName != null && !cachedTextureName.isEmpty();
            return new TextureResult(COMBINED_TEXTURE_CACHE.get(entityUUID), cachedTextureName, isPreset);
        }

        // Use the improved texture search method
        Path texturePath = findExistingTextureFile(entityUUID);
        if (texturePath != null) {
            MagicRealms.LOGGER.info("Found existing texture for entity {} at: {}", entityUUID, texturePath);

            // Update textureOutputDir to match the directory where we found the texture
            Path foundDir = texturePath.getParent();
            if (!foundDir.equals(textureOutputDir)) {
                MagicRealms.LOGGER.info("Updating texture output directory from {} to {}", textureOutputDir, foundDir);
                textureOutputDir = foundDir;
            }

            try {
                long fileSize = Files.size(texturePath);
                if (fileSize > 0) {
                    BufferedImage existingImage = ImageIO.read(texturePath.toFile());
                    if (existingImage != null && existingImage.getWidth() > 0 && existingImage.getHeight() > 0) {
                        ResourceLocation location = DynamicTextureManager.registerDynamicTexture(entityUUID, existingImage);
                        if (location != null) {
                            COMBINED_TEXTURE_CACHE.put(entityUUID, location);
                            MagicRealms.LOGGER.info("Successfully loaded existing texture from: {}", texturePath);

                            // Check if this entity had a preset texture name stored
                            String storedTextureName = ENTITY_TEXTURE_NAMES.get(entityUUID);
                            boolean isPreset = storedTextureName != null && !storedTextureName.isEmpty();

                            return new TextureResult(location, storedTextureName, isPreset);
                        }
                    } else {
                        MagicRealms.LOGGER.warn("Existing texture file appears corrupted for entity {}", entityUUID);

                        // Instead of deleting immediately, rename the corrupted file for debugging
                        try {
                            Path backupPath = texturePath.getParent().resolve(entityUUID + "_corrupted_" + System.currentTimeMillis() + ".png");
                            Files.move(texturePath, backupPath);
                            MagicRealms.LOGGER.info("Moved corrupted texture to: {}", backupPath);
                        } catch (IOException moveException) {
                            MagicRealms.LOGGER.error("Failed to backup corrupted texture: {}", moveException.getMessage());
                        }
                    }
                } else {
                    MagicRealms.LOGGER.warn("Texture file is empty for entity {}, will regenerate", entityUUID);
                    try {
                        Files.delete(texturePath);
                    } catch (IOException deleteError) {
                        MagicRealms.LOGGER.error("Failed to delete empty texture file: {}", deleteError.getMessage());
                    }
                }
            } catch (IOException e) {
                MagicRealms.LOGGER.error("Failed to load existing texture from {}: {}", texturePath, e.getMessage());
            }
        }

        // Generate new texture if we reach here
        try {
            TextureCreationResult result = createCompleteTextureWithName(gender, entityClass, hairTextureIndex);
            if (result != null && result.image != null) {
                ResourceLocation location = DynamicTextureManager.registerDynamicTexture(entityUUID, result.image);
                if (location != null) {
                    COMBINED_TEXTURE_CACHE.put(entityUUID, location);

                    // Store the texture name if it's a preset texture
                    if (result.isPresetTexture && result.textureName != null) {
                        ENTITY_TEXTURE_NAMES.put(entityUUID, result.textureName);
                    }

                    // Save to disk in the correct directory
                    if (textureOutputDir != null) {
                        try {
                            Path newTexturePath = textureOutputDir.resolve(entityUUID + "_complete.png");
                            ImageIO.write(result.image, "PNG", newTexturePath.toFile());
                            MagicRealms.LOGGER.debug("Created and saved new complete texture for entity: " + entityUUID + " at: " + newTexturePath);
                        } catch (IOException e) {
                            MagicRealms.LOGGER.error("Failed to save complete texture to disk for entity " + entityUUID, e);
                        }
                    }

                    return new TextureResult(location, result.textureName, result.isPresetTexture);
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to create complete texture for entity " + entityUUID, e);
        }

        return null;
    }

    // Internal class for texture creation results
    private static class TextureCreationResult {
        final BufferedImage image;
        final String textureName;
        final boolean isPresetTexture;

        TextureCreationResult(BufferedImage image, String textureName, boolean isPresetTexture) {
            this.image = image;
            this.textureName = textureName;
            this.isPresetTexture = isPresetTexture;
        }
    }

    private static TextureCreationResult createCompleteTextureWithName(Gender gender, EntityClass entityClass, int hairTextureIndex) {
        try {
            // Roll for texture type: 70% layered, 30% additional
            double roll = TEXTURE_RANDOM.nextDouble();
            boolean useAdditionalTexture = roll < ADDITIONAL_TEXTURE_CHANCE;

            MagicRealms.LOGGER.debug("Texture selection roll: {:.3f} (threshold: {:.3f}) - Using {}",
                    roll, ADDITIONAL_TEXTURE_CHANCE, useAdditionalTexture ? "additional texture" : "layered texture");

            // Try to use additional texture if selected
            if (useAdditionalTexture && LayeredTextureManager.hasAdditionalTextures(gender)) {
                LayeredTextureManager.TextureWithName additionalResult = LayeredTextureManager.getRandomAdditionalTextureWithName(gender);
                if (additionalResult != null && additionalResult.getTexture() != null) {
                    BufferedImage additionalImage = loadImage(additionalResult.getTexture());
                    if (additionalImage != null) {
                        MagicRealms.LOGGER.debug("Successfully loaded additional {} texture: {} with name: {}",
                                gender.getName(), additionalResult.getTexture(), additionalResult.getName());

                        // Apply arm fixes to additional texture as well
                        BufferedImage finalTexture = ArmBackTextureFixer.fixArmBackStripes(additionalImage);
                        return new TextureCreationResult(finalTexture, additionalResult.getName(), true);
                    } else {
                        MagicRealms.LOGGER.warn("Failed to load additional texture: {}, falling back to layered", additionalResult.getTexture());
                    }
                } else {
                    MagicRealms.LOGGER.warn("No additional texture found for gender: {}, falling back to layered", gender.getName());
                }
            } else if (useAdditionalTexture) {
                MagicRealms.LOGGER.debug("Additional texture requested but none available for gender: {}, using layered", gender.getName());
            }

            // Fallback to layered texture generation (original method)
            BufferedImage layeredTexture = createLayeredTexture(gender, entityClass, hairTextureIndex);
            return new TextureCreationResult(layeredTexture, null, false);

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error creating complete texture", e);
            return null;
        }
    }

    private static BufferedImage createCompleteTexture(Gender gender, EntityClass entityClass, int hairTextureIndex) {
        TextureCreationResult result = createCompleteTextureWithName(gender, entityClass, hairTextureIndex);
        return result != null ? result.image : null;
    }

    // Fallback initialization for when no world is loaded
    public static void initializeDirectories() {
        if (textureOutputDir == null) {
            try {
                Minecraft mc = Minecraft.getInstance();

                // First, try to determine if we're in a world and use the correct directory
                IntegratedServer server = mc.getSingleplayerServer();
                if (server != null) {
                    // We're in singleplayer - use the world directory
                    Path worldSaveDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
                    String worldName = server.getWorldData().getLevelName();
                    textureOutputDir = worldSaveDir.resolve("magic_realms_textures").resolve("entity").resolve("human");
                    Files.createDirectories(textureOutputDir);
                    MagicRealms.LOGGER.warn("Late initialization of world texture directory: {}", textureOutputDir);
                    currentWorldName = worldName;
                    return;
                } else if (mc.getCurrentServer() != null) {
                    // We're in multiplayer - use server-specific directory
                    String serverName = mc.getCurrentServer().name.replaceAll("[^a-zA-Z0-9._-]", "_");
                    Path gameDir = mc.gameDirectory.toPath();
                    textureOutputDir = gameDir.resolve("multiplayer_textures").resolve(serverName).resolve("magic_realms_textures").resolve("entity").resolve("human");
                    Files.createDirectories(textureOutputDir);
                    MagicRealms.LOGGER.warn("Late initialization of multiplayer texture directory: {}", textureOutputDir);
                    currentWorldName = "multiplayer_" + serverName;
                    return;
                }

                // Last resort fallback - this should rarely happen
                Path gameDir = mc.gameDirectory.toPath();
                textureOutputDir = gameDir.resolve("magic_realms_textures").resolve("entity").resolve("human");
                Files.createDirectories(textureOutputDir);
                MagicRealms.LOGGER.error("Using game directory fallback for textures (this may cause texture loss): {}", textureOutputDir);
                currentWorldName = null;

            } catch (IOException e) {
                MagicRealms.LOGGER.error("Failed to create fallback texture output directory", e);
            }
        }
    }

    // Called when leaving a world
    public static void onWorldUnload() {
        MagicRealms.LOGGER.debug("World unloaded, clearing texture cache but preserving texture files");
        clearCache();

        // Don't delete texture files - they should persist for when we rejoin the world
        // Reset world tracking
        currentWorldName = null;
        textureOutputDir = null;
    }

    private static Path findExistingTextureFile(String entityUUID) {
        // Try to find the texture in the correct world-specific location first
        try {
            Minecraft mc = Minecraft.getInstance();
            IntegratedServer server = mc.getSingleplayerServer();

            if (server != null) {
                // Singleplayer - check world directory
                Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .resolve("magic_realms_textures").resolve("entity").resolve("human");
                Path worldTexture = worldDir.resolve(entityUUID + "_complete.png");

                if (Files.exists(worldTexture)) {
                    MagicRealms.LOGGER.debug("Found texture in world directory: {}", worldTexture);
                    return worldTexture;
                } else {
                    MagicRealms.LOGGER.debug("Texture not found in world directory: {}", worldTexture);
                }
            } else if (mc.getCurrentServer() != null) {
                // Multiplayer - check server-specific directory
                String serverName = mc.getCurrentServer().name.replaceAll("[^a-zA-Z0-9._-]", "_");
                Path serverDir = mc.gameDirectory.toPath()
                        .resolve("multiplayer_textures").resolve(serverName)
                        .resolve("magic_realms_textures").resolve("entity").resolve("human");
                Path serverTexture = serverDir.resolve(entityUUID + "_complete.png");

                if (Files.exists(serverTexture)) {
                    MagicRealms.LOGGER.info("Found texture in multiplayer directory: {}", serverTexture);
                    return serverTexture;
                } else {
                    MagicRealms.LOGGER.debug("Texture not found in multiplayer directory: {}", serverTexture);
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Error while searching for texture in correct location: {}", e.getMessage());
        }

        // Fallback: check the current textureOutputDir (if set)
        if (textureOutputDir != null) {
            Path currentDirTexture = textureOutputDir.resolve(entityUUID + "_complete.png");
            if (Files.exists(currentDirTexture)) {
                MagicRealms.LOGGER.info("Found texture in current directory: {}", currentDirTexture);
                return currentDirTexture;
            }
        }

        // Last resort: check game directory fallback location
        try {
            Path fallbackDir = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("magic_realms_textures").resolve("entity").resolve("human");
            Path fallbackTexture = fallbackDir.resolve(entityUUID + "_complete.png");

            if (Files.exists(fallbackTexture)) {
                MagicRealms.LOGGER.warn("Found texture in fallback directory (this indicates a directory mismatch): {}", fallbackTexture);
                return fallbackTexture;
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Error while checking fallback texture location: {}", e.getMessage());
        }

        MagicRealms.LOGGER.info("No existing texture found for entity: {}", entityUUID);
        return null;
    }

    public static ResourceLocation getCombinedTextureWithHair(String entityUUID, Gender gender, EntityClass entityClass, int hairTextureIndex) {
        // SAFETY CHECK: Ensure we're using the correct world directory
        if (textureOutputDir == null) {
            MagicRealms.LOGGER.warn("Texture directory not set, attempting to initialize...");
            initializeDirectories();

            if (textureOutputDir == null) {
                MagicRealms.LOGGER.error("Failed to initialize texture directory!");
                return null;
            }
        }

        // ADDITIONAL SAFETY: Verify we're using the correct world directory, not the fallback
        try {
            Minecraft mc = Minecraft.getInstance();
            IntegratedServer server = mc.getSingleplayerServer();

            if (server != null) {
                // Check if current directory matches the actual world directory
                Path actualWorldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .resolve("magic_realms_textures").resolve("entity").resolve("human");

                if (!textureOutputDir.equals(actualWorldDir)) {
                    MagicRealms.LOGGER.warn("Texture directory mismatch detected!");
                    MagicRealms.LOGGER.warn("Current: {}", textureOutputDir);
                    MagicRealms.LOGGER.warn("Expected: {}", actualWorldDir);
                    MagicRealms.LOGGER.warn("Correcting texture directory...");

                    // Update to correct directory
                    textureOutputDir = actualWorldDir;
                    Files.createDirectories(textureOutputDir);
                    currentWorldName = server.getWorldData().getLevelName();

                    MagicRealms.LOGGER.info("Corrected texture directory to: {}", textureOutputDir);
                }
            } else if (mc.getCurrentServer() != null) {
                // Similar check for multiplayer
                String serverName = mc.getCurrentServer().name.replaceAll("[^a-zA-Z0-9._-]", "_");
                Path expectedMultiplayerDir = mc.gameDirectory.toPath()
                        .resolve("multiplayer_textures").resolve(serverName)
                        .resolve("magic_realms_textures").resolve("entity").resolve("human");

                if (!textureOutputDir.equals(expectedMultiplayerDir)) {
                    MagicRealms.LOGGER.warn("Multiplayer texture directory mismatch detected!");
                    MagicRealms.LOGGER.warn("Current: {}", textureOutputDir);
                    MagicRealms.LOGGER.warn("Expected: {}", expectedMultiplayerDir);

                    textureOutputDir = expectedMultiplayerDir;
                    Files.createDirectories(textureOutputDir);
                    currentWorldName = "multiplayer_" + serverName;

                    MagicRealms.LOGGER.info("Corrected multiplayer texture directory to: {}", textureOutputDir);
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error during texture directory validation: {}", e.getMessage());
        }

        MagicRealms.LOGGER.debug("Using texture directory: {}", textureOutputDir);

        // Track this entity as active
        ACTIVE_ENTITIES.add(entityUUID);

        if (COMBINED_TEXTURE_CACHE.containsKey(entityUUID)) {
            MagicRealms.LOGGER.debug("Using cached complete texture for entity: " + entityUUID);
            return COMBINED_TEXTURE_CACHE.get(entityUUID);
        }

        // Use the improved texture search method
        Path texturePath = findExistingTextureFile(entityUUID);
        if (texturePath != null) {
            MagicRealms.LOGGER.debug("Found existing texture for entity {} at: {}", entityUUID, texturePath);

            // Update textureOutputDir to match the directory where we found the texture
            Path foundDir = texturePath.getParent();
            if (!foundDir.equals(textureOutputDir)) {
                MagicRealms.LOGGER.debug("Updating texture output directory from {} to {}", textureOutputDir, foundDir);
                textureOutputDir = foundDir;
            }

            try {
                long fileSize = Files.size(texturePath);
                if (fileSize > 0) {
                    BufferedImage existingImage = ImageIO.read(texturePath.toFile());
                    if (existingImage != null && existingImage.getWidth() > 0 && existingImage.getHeight() > 0) {
                        ResourceLocation location = DynamicTextureManager.registerDynamicTexture(entityUUID, existingImage);
                        if (location != null) {
                            COMBINED_TEXTURE_CACHE.put(entityUUID, location);
                            MagicRealms.LOGGER.debug("Successfully loaded existing texture from: {}", texturePath);
                            return location;
                        }
                    } else {
                        MagicRealms.LOGGER.warn("Existing texture file appears corrupted for entity {}", entityUUID);

                        // Instead of deleting immediately, rename the corrupted file for debugging
                        try {
                            Path backupPath = texturePath.getParent().resolve(entityUUID + "_corrupted_" + System.currentTimeMillis() + ".png");
                            Files.move(texturePath, backupPath);
                            MagicRealms.LOGGER.debug("Moved corrupted texture to: {}", backupPath);
                        } catch (IOException moveException) {
                            MagicRealms.LOGGER.error("Failed to backup corrupted texture: {}", moveException.getMessage());
                        }
                    }
                } else {
                    MagicRealms.LOGGER.warn("Texture file is empty for entity {}, will regenerate", entityUUID);
                    try {
                        Files.delete(texturePath);
                    } catch (IOException deleteError) {
                        MagicRealms.LOGGER.error("Failed to delete empty texture file: {}", deleteError.getMessage());
                    }
                }
            } catch (IOException e) {
                MagicRealms.LOGGER.error("Failed to load existing texture from {}: {}", texturePath, e.getMessage());
            }
        }

        // Generate new texture if we reach here
        try {
            BufferedImage combinedImage = createCompleteTexture(gender, entityClass, hairTextureIndex);
            if (combinedImage != null) {
                ResourceLocation location = DynamicTextureManager.registerDynamicTexture(entityUUID, combinedImage);
                if (location != null) {
                    COMBINED_TEXTURE_CACHE.put(entityUUID, location);

                    // Save to disk in the correct directory
                    if (textureOutputDir != null) {
                        try {
                            Path newTexturePath = textureOutputDir.resolve(entityUUID + "_complete.png");
                            ImageIO.write(combinedImage, "PNG", newTexturePath.toFile());
                            MagicRealms.LOGGER.debug("Created and saved new complete texture for entity: " + entityUUID + " at: " + newTexturePath);
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

    private static BufferedImage createLayeredTexture(Gender gender, EntityClass entityClass, int hairTextureIndex) {
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
            MagicRealms.LOGGER.debug("Successfully created layered texture with all layers including hair");

            return finalTexture;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error creating layered texture", e);
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
        removeEntityTexture(entityUUID, false);
    }

    public static void removeEntityTexture(String entityUUID, boolean entityDespawned) {
        try {
            // Remove from active tracking
            ACTIVE_ENTITIES.remove(entityUUID);

            // Remove from cache
            COMBINED_TEXTURE_CACHE.remove(entityUUID);

            // Remove texture name mapping
            ENTITY_TEXTURE_NAMES.remove(entityUUID);

            DynamicTextureManager.unregisterTexture(entityUUID);

            // Only delete file if entity truly despawned/died
            if (entityDespawned && textureOutputDir != null) {
                Path completeTexturePath = textureOutputDir.resolve(entityUUID + "_complete.png");
                if (Files.exists(completeTexturePath)) {
                    Files.delete(completeTexturePath);
                    MagicRealms.LOGGER.debug("Deleted texture file for despawned entity: " + entityUUID);
                }
            } else {
                MagicRealms.LOGGER.debug("Removed texture from cache for entity: " + entityUUID + " (file preserved)");
            }
        } catch (IOException e) {
            MagicRealms.LOGGER.error("Failed to delete complete texture for entity " + entityUUID, e);
        }
    }

    public static void markEntityInactive(String entityUUID) {
        ACTIVE_ENTITIES.remove(entityUUID);
        MagicRealms.LOGGER.debug("Marked entity {} as inactive", entityUUID);
    }

    public static boolean isEntityActive(String entityUUID) {
        return ACTIVE_ENTITIES.contains(entityUUID);
    }

    public static void clearCache() {
        COMBINED_TEXTURE_CACHE.clear();
        ACTIVE_ENTITIES.clear();
        DynamicTextureManager.clearAllTextures();
    }

    // Configuration methods
    public static double getAdditionalTextureChance() {
        return ADDITIONAL_TEXTURE_CHANCE;
    }
}