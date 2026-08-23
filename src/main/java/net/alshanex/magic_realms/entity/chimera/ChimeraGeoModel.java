package net.alshanex.magic_realms.entity.chimera;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ChimeraGeoModel extends GeoModel<ChimeraEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "geo/chimera.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/chimera_fallback.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "animations/chimera.animation.json");

    @Override
    public ResourceLocation getModelResource(ChimeraEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ChimeraEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ChimeraEntity entity) {
        return ANIMATION;
    }
}
