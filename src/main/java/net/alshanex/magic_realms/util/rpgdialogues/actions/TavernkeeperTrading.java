package net.alshanex.magic_realms.util.rpgdialogues.actions;

import com.mojang.serialization.MapCodec;
import net.alshanex.magic_realms.entity.tavernkeep.TavernKeeperEntity;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueAction;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueContext;

public record TavernkeeperTrading() implements DialogueAction {
    public static final MapCodec<TavernkeeperTrading> CODEC = MapCodec.unit(new TavernkeeperTrading());
    @Override public MapCodec<? extends DialogueAction> codec() { return CODEC; }
    @Override public void run(DialogueContext context) {
        if(context.speaker() instanceof TavernKeeperEntity tavernKeeper){
            tavernKeeper.tryToTrade(context.player());
        }
    }
}
