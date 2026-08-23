package net.alshanex.magic_realms.util.chimera;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.List;

public record ChimeraPartSource(
        List<ModelPart> vanillaParts,
        List<GeoBone> geoParts,
        ResourceLocation texture,
        float scale,
        float offsetX,
        float offsetY,
        float offsetZ,
        boolean isGeckoLib,
        ResourceLocation geoModelResource,
        float legHeight
) {
    public static ChimeraPartSource vanilla(List<ModelPart> parts, ResourceLocation texture,
                                            float scale, float offsetX, float offsetY, float offsetZ) {
        return new ChimeraPartSource(parts, List.of(), texture, scale,
                offsetX, offsetY, offsetZ, false, null, 12.0f);
    }

    public static ChimeraPartSource vanilla(List<ModelPart> parts, ResourceLocation texture,
                                            float scale, float offsetX, float offsetY, float offsetZ,
                                            float legHeight) {
        return new ChimeraPartSource(parts, List.of(), texture, scale,
                offsetX, offsetY, offsetZ, false, null, legHeight);
    }

    public static ChimeraPartSource geckoLib(List<GeoBone> bones, ResourceLocation texture,
                                             float scale, float offsetX, float offsetY, float offsetZ,
                                             ResourceLocation geoModelResource, float legHeight) {
        return new ChimeraPartSource(List.of(), bones, texture, scale,
                offsetX, offsetY, offsetZ, true, geoModelResource, legHeight);
    }

    public float getRenderedLegHeight() {
        return legHeight * scale;
    }

    public float getLegHeightRatio() {
        return getRenderedLegHeight() / 12.0f;
    }
}
