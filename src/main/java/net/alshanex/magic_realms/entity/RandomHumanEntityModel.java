package net.alshanex.magic_realms.entity;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.minecraft.resources.ResourceLocation;

public class RandomHumanEntityModel extends AbstractSpellCastingMobModel {
    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob object) {
        return ((RandomHumanEntity)object).getTextureConfig().getSkinTexture();
    }
}
