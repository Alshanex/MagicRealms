package net.alshanex.magic_realms.util.rpgdialogues.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.data.PlayerLoreProgress;
import net.alshanex.magic_realms.story.StoryHelper;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueCondition;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueContext;

public record TavernkeeperTipConditionFalse(ResourceLocation story, int chapter) implements DialogueCondition {
    public static final MapCodec<TavernkeeperTipConditionFalse> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("story").forGetter(TavernkeeperTipConditionFalse::story),
                    Codec.INT.fieldOf("chapter").forGetter(TavernkeeperTipConditionFalse::chapter))
                    .apply(instance, TavernkeeperTipConditionFalse::new));
    @Override
    public MapCodec<? extends DialogueCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(DialogueContext dialogueContext) {
        PlayerLoreProgress progress = StoryHelper.getPlayerLoreProgress(dialogueContext.player());
        return !progress.hasPage(story, chapter);
    }
}
