package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;
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

    public static int getRandomHairTextureIndex(String category) {
        List<ResourceLocation> textures = TEXTURE_CACHE.get(category);
        if (textures == null || textures.isEmpty()) {
            MagicRealms.LOGGER.debug("No textures found for category: " + category);
            return -1;
        }
        int index = new Random().nextInt(textures.size());
        //MagicRealms.LOGGER.debug("Selected hair texture index {} from {} options for category: {}", index, textures.size(), category);
        return index;
    }

    public static ResourceLocation getTextureByIndex(String category, int index) {
        List<ResourceLocation> textures = TEXTURE_CACHE.get(category);
        if (textures == null || textures.isEmpty()) {
            MagicRealms.LOGGER.debug("No textures found for category: " + category);
            return null;
        }
        if (index < 0 || index >= textures.size()) {
            MagicRealms.LOGGER.warn("Invalid texture index {} for category {} (max: {})",
                    index, category, textures.size() - 1);
            return null;
        }
        return textures.get(index);
    }

    public static ResourceLocation getRandomTexture(String category) {
        List<ResourceLocation> textures = TEXTURE_CACHE.get(category);
        if (textures == null || textures.isEmpty()) {
            MagicRealms.LOGGER.debug("No textures found for category: " + category);
            return null;
        }
        return textures.get(new Random().nextInt(textures.size()));
    }

    // New method that returns both texture and name
    public static TextureWithName getRandomAdditionalTextureWithName(Gender gender) {
        String category = "additional_" + gender.getName();
        List<ResourceLocation> textures = TEXTURE_CACHE.get(category);

        if (textures == null || textures.isEmpty()) {
            MagicRealms.LOGGER.debug("No additional textures found for gender: " + gender.getName());
            return null;
        }

        ResourceLocation selectedTexture = textures.get(new Random().nextInt(textures.size()));

        // Extract the texture name from the ResourceLocation path
        String textureName = extractTextureNameFromPath(selectedTexture.getPath());

        MagicRealms.LOGGER.debug("Selected additional texture: {} with name: {} for gender: {}",
                selectedTexture, textureName, gender.getName());

        return new TextureWithName(selectedTexture, textureName);
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
