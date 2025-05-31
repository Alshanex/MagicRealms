package net.alshanex.magic_realms.traits;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.legendary.LegendaryTrait;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class Pickpocket extends MobTrait {
    public Pickpocket(ChatFormatting format) {
        super(format);
    }

    @Override
    public boolean onAttackedByOthers(int level, LivingEntity entity, DamageData.Attack event) {
        Random random = new Random();
        int roll = random.nextInt(100);

        if(roll <= 50 && event.getSource().getEntity() instanceof LivingEntity attacker){
            Collection<MobEffectInstance> activeEffects = attacker.getActiveEffects();
            if(!activeEffects.isEmpty()){
                List<MobEffectInstance> effectList = new ArrayList<>(activeEffects);

                Random randomEffect = new Random();
                int randomIndex = randomEffect.nextInt(effectList.size());

                MobEffectInstance stolenEffect = effectList.get(randomIndex);
                attacker.removeEffect(stolenEffect.getEffect());
                event.getTarget().addEffect(stolenEffect);
            }
        }
        return false;
    }
}
