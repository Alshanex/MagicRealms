package net.alshanex.magic_realms.util.rpgdialogues.values;

import com.mojang.serialization.MapCodec;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueContext;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueValue;

public record TimeUntilPermanentContract() implements DialogueValue {
    public static final MapCodec<TimeUntilPermanentContract> CODEC = MapCodec.unit(new TimeUntilPermanentContract());
    @Override
    public MapCodec<? extends DialogueValue> codec() {
        return CODEC;
    }

    @Override
    public Component resolve(DialogueContext dialogueContext) {
        if(dialogueContext.speaker() instanceof AbstractMercenaryEntity mercenary && !dialogueContext.player().level().isClientSide() ){
            ContractData contractData = mercenary.getData(MRDataAttachments.CONTRACT_DATA);
            if (!contractData.hasMinimumContractTime(dialogueContext.player().getUUID(), dialogueContext.player().level())) {
                int remainingMinutes = contractData.getRemainingMinutesForPermanent(dialogueContext.player().getUUID(), dialogueContext.player().level());
                return Component.literal(String.valueOf(remainingMinutes));
            }
        }
        return Component.empty();
    }
}
