package net.alshanex.magic_realms.util.rpgdialogues.conditions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueCondition;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueContext;

public record EnityTypeCondition(ResourceLocation entityId) implements DialogueCondition {
    public static final MapCodec<EnityTypeCondition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("entityId").forGetter(EnityTypeCondition::entityId))
                    .apply(instance, EnityTypeCondition::new));
    @Override
    public MapCodec<? extends DialogueCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(DialogueContext dialogueContext) {
        ResourceLocation targetEntityId = BuiltInRegistries.ENTITY_TYPE.getKey(dialogueContext.speaker().getType());
        return entityId.equals(targetEntityId);
    }
}
