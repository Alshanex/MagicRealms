package net.alshanex.magic_realms.entity.random;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.alshanex.magic_realms.util.humans.mercenaries.skins_management.TextureComponents;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RandomHumanEntityRenderer extends AbstractMercenaryEntityRenderer {

    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");

    public RandomHumanEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RandomHumanEntityModel());

        this.addRenderLayer(new MercenarySkinLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSpellCastingMob entity) {
        if (!(entity instanceof RandomHumanEntity human)) {
            return DEFAULT_TEXTURE;
        }

        TextureComponents components = human.getTextureComponents();
        if (components == null) {
            return DEFAULT_TEXTURE;
        }

        // Preset path: single complete texture, used for the whole model.
        if (components.isPresetTexture()) {
            String tex = components.getSkinTexture();
            if (tex == null) return DEFAULT_TEXTURE;
            try {
                return toAssetPath(tex);
            } catch (Exception e) {
                MagicRealms.LOGGER.warn("Invalid preset texture for {}: {}",
                        human.getEntityName(), tex);
                return DEFAULT_TEXTURE;
            }
        }

        // Layered path: return the base skin. MercenarySkinLayer handles the overlays.
        String skinTex = components.getSkinTexture();
        if (skinTex == null) {
            return DEFAULT_TEXTURE;
        }
        try {
            return toAssetPath(skinTex);
        } catch (Exception e) {
            MagicRealms.LOGGER.warn("Invalid skin texture for {}: {}", human.getEntityName(), skinTex);
            return DEFAULT_TEXTURE;
        }
    }

    static ResourceLocation toAssetPath(String textureId) {
        ResourceLocation parsed = ResourceLocation.parse(textureId);
        return ResourceLocation.fromNamespaceAndPath(
                parsed.getNamespace(),
                "textures/" + parsed.getPath() + ".png"
        );
    }
}