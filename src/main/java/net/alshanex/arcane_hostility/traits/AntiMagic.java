package net.alshanex.arcane_hostility.traits;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;

public class AntiMagic extends LegendaryTrait {
    public AntiMagic(ChatFormatting format) {
        super(format);
    }

    @Override
    public boolean onAttackedByOthers(int level, LivingEntity entity, DamageData.Attack event) {
        if(event.getAttacker() instanceof IMagicSummon summon){
            summon.onUnSummon();
            return true;
        }
        return false;
    }
}
