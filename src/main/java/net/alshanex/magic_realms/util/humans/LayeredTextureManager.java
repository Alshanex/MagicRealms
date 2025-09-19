package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class LayeredTextureManager {
    public static final Map<String, List<ResourceLocation>> TEXTURE_CACHE = new HashMap<>();

    // New class to hold texture and its name
    public static class TextureWithName {
        private final ResourceLocation texture;
        private final String name;

        public TextureWithName(ResourceLocation texture, String name) {
            this.texture = texture;
            this.name = name;
        }

        public ResourceLocation getTexture() {
            return texture;
        }

        public String getName() {
            return name;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void loadTextures() {
        MagicRealms.LOGGER.debug("Starting to load layered textures...");

        loadTexturesFromDirectory("textures/entity/human/skin", "skin");
        loadTexturesFromDirectory("textures/entity/human/eyes", "eyes");
        loadTexturesFromDirectory("textures/entity/human/hair/female", "hair_female");
        loadTexturesFromDirectory("textures/entity/human/hair/male", "hair_male");
        loadTexturesFromDirectory("textures/entity/human/clothes/common/female", "clothes_common_female");
        loadTexturesFromDirectory("textures/entity/human/clothes/common/male", "clothes_common_male");
        loadTexturesFromDirectory("textures/entity/human/clothes/mage/female", "clothes_mage_female");
        loadTexturesFromDirectory("textures/entity/human/clothes/mage/male", "clothes_mage_male");
        loadTexturesFromDirectory("textures/entity/human/clothes/rogue/female", "clothes_rogue_female");
        loadTexturesFromDirectory("textures/entity/human/clothes/rogue/male", "clothes_rogue_male");
        loadTexturesFromDirectory("textures/entity/human/clothes/warrior/female", "clothes_warrior_female");
        loadTexturesFromDirectory("textures/entity/human/clothes/warrior/male", "clothes_warrior_male");

        // Load additional complete textures
        loadTexturesFromDirectory("textures/entity/human/additional_textures/male", "additional_male");
        loadTexturesFromDirectory("textures/entity/human/additional_textures/female", "additional_female");

        MagicRealms.LOGGER.debug("Finished loading textures. Total categories: " + TEXTURE_CACHE.size());
    }

    private static void loadTexturesFromDirectory(String path, String category) {
        List<ResourceLocation> textures = new ArrayList<>();

        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                net.minecraft.server.packs.resources.ResourceManager resourceManager = mc.getResourceManager();

                Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> resources = resourceManager.listResources(
                        path,
                        location -> location.getPath().endsWith(".png")
                );

                for (ResourceLocation resource : resources.keySet()) {
                    textures.add(resource);
                }

                MagicRealms.LOGGER.debug("Loaded " + textures.size() + " textures for category: " + category);
                if (textures.isEmpty()) {
                    MagicRealms.LOGGER.debug("Warning: No textures found in directory: " + path);
                }
            } else {
                MagicRealms.LOGGER.debug("Minecraft instance or ResourceManager is null - cannot load textures");
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Error loading textures from directory " + path + ": " + e.getMessage());
        }

        TEXTURE_CACHE.put(category, textures);
    }

    // Existing random methods for backward compatibility
    public static TextureWithName getRandomAdditionalTextureWithName(Gender gender, RandomSource random) {
        String category = "additional_" + gender.getName();
        List<ResourceLocation> textures = TEXTURE_CACHE.get(category);

        if (textures == null || textures.isEmpty()) {
            MagicRealms.LOGGER.debug("No additional textures found for gender: " + gender.getName());
            return null;
        }

        ResourceLocation selectedTexture = textures.get(random.nextInt(textures.size()));
        String textureName = extractTextureNameFromPath(selectedTexture.getPath());

        MagicRealms.LOGGER.debug("Selected random additional texture: {} with name: {} for gender: {}",
                selectedTexture, textureName, gender.getName());

        return new TextureWithName(selectedTexture, textureName);
    }

    // NEW: Index-based methods for deterministic texture selection
    public static String getTextureByIndex(String category, int index) {
        List<ResourceLocation> textures = TEXTURE_CACHE.get(category);
        if (textures == null || textures.isEmpty()) {
            MagicRealms.LOGGER.debug("No textures found for category: {}", category);
            return null;
        }

        int actualIndex = Math.abs(index % textures.size());
        ResourceLocation selectedTexture = textures.get(actualIndex);

        MagicRealms.LOGGER.debug("Selected texture by index for {}: {} (index {} -> {})",
                category, selectedTexture, index, actualIndex);

        return selectedTexture.toString();
    }

    public static TextureWithName getAdditionalTextureByIndex(Gender gender, int index) {
        String category = "additional_" + gender.getName();
        List<ResourceLocation> textures = TEXTURE_CACHE.get(category);

        if (textures == null || textures.isEmpty()) {
            MagicRealms.LOGGER.debug("No additional textures found for gender: {}", gender.getName());
            return null;
        }

        int actualIndex = Math.abs(index % textures.size());
        ResourceLocation selectedTexture = textures.get(actualIndex);
        String textureName = extractTextureNameFromPath(selectedTexture.getPath());

        MagicRealms.LOGGER.debug("Selected additional texture by index for {}: {} with name: {} (index {} -> {})",
                gender.getName(), selectedTexture, textureName, index, actualIndex);

        return new TextureWithName(selectedTexture, textureName);
    }

    public static String getClothesTextureByIndex(Gender gender, EntityClass entityClass, int index) {
        List<ResourceLocation> availableTextures = new ArrayList<>();

        // Try class-specific textures first
        List<ResourceLocation> classTextures = TEXTURE_CACHE.get(
                "clothes_" + entityClass.getName() + "_" + gender.getName());
        if (classTextures != null && !classTextures.isEmpty()) {
            availableTextures.addAll(classTextures);
        }

        // Fallback to common textures if no class-specific ones
        if (availableTextures.isEmpty()) {
            List<ResourceLocation> commonTextures = TEXTURE_CACHE.get(
                    "clothes_common_" + gender.getName());
            if (commonTextures != null) {
                availableTextures.addAll(commonTextures);
            }
        }

        if (availableTextures.isEmpty()) {
            MagicRealms.LOGGER.debug("No clothes textures found for {} {}", entityClass.getName(), gender.getName());
            return null;
        }

        int actualIndex = Math.abs(index % availableTextures.size());
        ResourceLocation selectedTexture = availableTextures.get(actualIndex);

        MagicRealms.LOGGER.debug("Selected clothes texture by index for {} {}: {} (index {} -> {})",
                entityClass.getName(), gender.getName(), selectedTexture, index, actualIndex);

        return selectedTexture.toString();
    }

    public static String getHairTextureByIndex(Gender gender, int index) {
        String category = "hair_" + gender.getName();
        List<ResourceLocation> textures = TEXTURE_CACHE.get(category);

        if (textures == null || textures.isEmpty()) {
            MagicRealms.LOGGER.debug("No hair textures found for gender: {}", gender.getName());
            return null;
        }

        int actualIndex = Math.abs(index % textures.size());
        ResourceLocation selectedTexture = textures.get(actualIndex);

        MagicRealms.LOGGER.debug("Selected hair texture by index for {}: {} (index {} -> {})",
                gender.getName(), selectedTexture, index, actualIndex);

        return selectedTexture.toString();
    }

    // Helper method to extract a clean texture name from the file path
    private static String extractTextureNameFromPath(String path) {
        try {
            // Extract the filename from the path
            String filename = path.substring(path.lastIndexOf('/') + 1);

            // Remove the .png extension
            if (filename.endsWith(".png")) {
                filename = filename.substring(0, filename.length() - 4);
            }

            // Convert underscores to spaces and capitalize each word for a prettier name
            String[] words = filename.split("_");
            StringBuilder prettyName = new StringBuilder();

            for (int i = 0; i < words.length; i++) {
                if (i > 0) {
                    prettyName.append(" ");
                }

                String word = words[i];
                if (!word.isEmpty()) {
                    // Capitalize first letter, lowercase the rest
                    prettyName.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase());
                }
            }

            return prettyName.toString();

        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Failed to extract texture name from path: " + path, e);
            return "Unknown";
        }
    }

    // Utility methods
    public static boolean areTexturesLoaded() {
        return !TEXTURE_CACHE.isEmpty();
    }

    public static void ensureTexturesLoaded() {
        if (!areTexturesLoaded()) {
            loadTextures();
        }
    }

    public static boolean hasAdditionalTextures(Gender gender) {
        String category = "additional_" + gender.getName();
        List<ResourceLocation> textures = TEXTURE_CACHE.get(category);
        return textures != null && !textures.isEmpty();
    }

    public static int getAdditionalTextureCount(Gender gender) {
        String category = "additional_" + gender.getName();
        List<ResourceLocation> textures = TEXTURE_CACHE.get(category);
        return textures != null ? textures.size() : 0;
    }

    public static void clearCache() {
        TEXTURE_CACHE.clear();
    }
}