package net.alshanex.arcane_hostility.traits;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.alshanex.arcane_hostility.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;

public class Sneaky extends MobTrait {
    public Sneaky(ChatFormatting format) {
        super(format);
    }

    @Override
    public boolean onAttackedByOthers(int level, LivingEntity entity, DamageData.Attack event) {
        int distance = Config.distance;
        if(event.getAttacker().distanceToSqr(event.getTarget()) > (distance * distance)){
            return !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) &&
                    !event.getSource().is(DamageTypeTags.BYPASSES_EFFECTS);
        }
        return false;
    }
}
