package net.alshanex.magic_realms.entity.exclusive;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntityModel;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractFixedTextureRenderer extends AbstractMercenaryEntityRenderer {

    public AbstractFixedTextureRenderer(EntityRendererProvider.Context renderManager,
                                        AbstractMercenaryEntityModel model) {
        super(renderManager, model);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSpellCastingMob entity) {
        return getOriginalTexture();
    }

    protected abstract ResourceLocation getOriginalTexture();

    public static void clearTextureCache() {
        // intentional no-op
    }

    public static void clearAllCaches() {
        // intentional no-op
    }

    public static String getCacheStats() {
        return "no cache (post-cleanup)";
    }
}
