package net.alshanex.magic_realms.util.humans;

import com.mojang.blaze3d.platform.NativeImage;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class DynamicTextureManager {
    private static final Map<String, ResourceLocation> REGISTERED_TEXTURES = new ConcurrentHashMap<>();

    public static ResourceLocation registerDynamicTexture(String entityUUID, BufferedImage combinedImage) {
        String textureId = "dynamic_human_" + entityUUID;
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, textureId);

        if (REGISTERED_TEXTURES.containsKey(entityUUID)) {
            return REGISTERED_TEXTURES.get(entityUUID);
        }

        try {
            // Convertir BufferedImage a NativeImage
            NativeImage nativeImage = bufferedImageToNativeImage(combinedImage);

            // Crear y registrar la textura dinámica
            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
            Minecraft.getInstance().getTextureManager().register(location, dynamicTexture);

            REGISTERED_TEXTURES.put(entityUUID, location);
            MagicRealms.LOGGER.debug("Registered dynamic texture for entity: " + entityUUID);

            return location;

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Failed to register dynamic texture for entity " + entityUUID, e);
            return null;
        }
    }

    public static void unregisterTexture(String entityUUID) {
        ResourceLocation location = REGISTERED_TEXTURES.remove(entityUUID);
        if (location != null) {
            try {
                Minecraft.getInstance().getTextureManager().release(location);
                MagicRealms.LOGGER.debug("Unregistered texture for entity: " + entityUUID);
            } catch (Exception e) {
                MagicRealms.LOGGER.error("Failed to unregister texture for entity " + entityUUID, e);
            }
        }
    }

    private static NativeImage bufferedImageToNativeImage(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "PNG", baos);
        byte[] bytes = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return NativeImage.read(bais);
    }

    public static void clearAllTextures() {
        for (String entityUUID : REGISTERED_TEXTURES.keySet()) {
            unregisterTexture(entityUUID);
        }
        REGISTERED_TEXTURES.clear();
    }

    public static boolean hasTexture(String entityUUID) {
        return REGISTERED_TEXTURES.containsKey(entityUUID);
    }
}
