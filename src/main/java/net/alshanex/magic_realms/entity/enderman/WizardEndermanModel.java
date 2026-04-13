package net.alshanex.magic_realms.entity.enderman;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.WalkAnimationState;
import org.joml.Vector2f;

public class WizardEndermanModel extends AbstractSpellCastingMobModel {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/enderman/enderman.png");
    public static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "geo/wizard_enderman.geo.json");

    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getModelResource(AbstractSpellCastingMob object) {
        return MODEL;
    }

    @Override
    protected Vector2f getLimbSwing(AbstractSpellCastingMob entity,
                                    WalkAnimationState walkAnimationState, float partialTick) {
        return new Vector2f(0, 0);
    }
}
