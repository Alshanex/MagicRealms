package net.alshanex.magic_realms.traits;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;

public class AntiMagic extends LegendaryTrait {
    public AntiMagic(ChatFormatting format) {
        super(format);
    }

    @Override
    public boolean onAttackedByOthers(int level, LivingEntity entity, DamageData.Attack event) {
        if(event.getAttacker() instanceof IMagicSummon summon){
            summon.onUnSummon();
            return !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) &&
                    !event.getSource().is(DamageTypeTags.BYPASSES_EFFECTS);
        }
        return false;
    }
}
