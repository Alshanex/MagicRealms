package net.alshanex.magic_realms.entity.exclusive;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.random.RandomHumanEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class AlshanexEntityModel extends AbstractSpellCastingMobModel {
    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob entity) {
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/alshanex.png");
    }
}
