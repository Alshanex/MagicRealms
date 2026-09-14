package net.alshanex.magic_realms.util.rpgdialogues.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.humans.mercenaries.EntityClass;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueCondition;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueContext;

public record MercenaryClassCondition(String mercenaryClass) implements DialogueCondition {
    public static final MapCodec<MercenaryClassCondition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.fieldOf("mercenaryClass").forGetter(MercenaryClassCondition::mercenaryClass))
                    .apply(instance, MercenaryClassCondition::new));
    @Override
    public MapCodec<? extends DialogueCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(DialogueContext dialogueContext) {
        if(dialogueContext.speaker() instanceof AbstractMercenaryEntity mercenary){
            EntityClass mercenaryEntityClass = mercenary.getEntityClass();
            if(mercenaryEntityClass == EntityClass.WARRIOR){
                if(mercenary.hasShield()){
                    return mercenaryClass.equals("tank");
                }
            } else if (mercenaryEntityClass == EntityClass.ROGUE){
                if(mercenary.isArcher()){
                    return mercenaryClass.equals("archer");
                }
            }
            return mercenary.getEntityClass().getName().equals(mercenaryClass);
        }
        return false;
    }
}
