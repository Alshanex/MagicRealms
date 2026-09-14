package net.alshanex.magic_realms.util.rpgdialogues.conditions;

import com.mojang.serialization.MapCodec;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueCondition;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueContext;

public record MercenaryCombatCondition() implements DialogueCondition {
    public static final MapCodec<MercenaryCombatCondition> CODEC = MapCodec.unit(new MercenaryCombatCondition());
    @Override public MapCodec<? extends DialogueCondition> codec() { return CODEC; }

    @Override
    public boolean test(DialogueContext dialogueContext) {
        if(dialogueContext.speaker() instanceof AbstractMercenaryEntity mercenary){
            return mercenary.isInCombat();
        }
        return false;
    }
}
