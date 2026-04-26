package net.alshanex.magic_realms.entity.exclusive.jara;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntityModel;
import net.minecraft.resources.ResourceLocation;

public class JaraEntityModel extends AbstractMercenaryEntityModel {
    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob entity) {
        return ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "textures/entity/exclusive_mercenaries/jara.png");
    }
}
