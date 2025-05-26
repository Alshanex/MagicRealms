package net.alshanex.magic_realms.entity;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RandomHumanEntityRenderer extends AbstractSpellCastingMobRenderer {
    public RandomHumanEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RandomHumanEntityModel());
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSpellCastingMob animatable) {
        return ((RandomHumanEntity)animatable).getTextureConfig().getSkinTexture();
    }
}
