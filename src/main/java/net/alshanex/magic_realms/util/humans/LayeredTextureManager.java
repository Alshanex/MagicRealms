package net.alshanex.magic_realms.util.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class LayeredTextureManager {
    public static final Map<String, List<ResourceLocation>> TEXTURE_CACHE = new HashMap<>();

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
        MagicRealms.LOGGER.debug("Selected hair texture index {} from {} options for category: {}",
                index, textures.size(), category);
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

    public static void clearCache() {
        TEXTURE_CACHE.clear();
    }
}
