package net.alshanex.magic_realms.util.rpgdialogues.values;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alshanex.magic_realms.data.PersonalityData;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.entity.humans.IExclusiveMercenary;
import net.alshanex.magic_realms.entity.humans.exclusive.ace.AceEntity;
import net.alshanex.magic_realms.entity.humans.exclusive.aliana.AlianaEntity;
import net.alshanex.magic_realms.entity.humans.exclusive.jara.JaraEntity;
import net.alshanex.magic_realms.entity.humans.exclusive.lilac.LilacEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.Archetype;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.Hobby;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueContext;
import net.pixeldreamstudios.rpgdialogue.dialogue.DialogueValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public record MercenarySpeech(int possibleResponsesAmount) implements DialogueValue {
    public static final MapCodec<MercenarySpeech> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                            Codec.INT.fieldOf("possibleResponsesAmount").forGetter(MercenarySpeech::possibleResponsesAmount))
                    .apply(instance, MercenarySpeech::new));
    @Override
    public MapCodec<? extends DialogueValue> codec() {
        return CODEC;
    }

    @Override
    public Component resolve(DialogueContext dialogueContext) {
        if(dialogueContext.speaker() instanceof AbstractMercenaryEntity mercenary){
            List<String> result = new ArrayList<>();

            PersonalityData personality = mercenary.getData(MRDataAttachments.PERSONALITY);
            if (personality != null && personality.isInitialized()) {
                Hobby hobby = personality.getHobby(false);
                Archetype archetype = personality.getArchetype(false);

                if (hobby != null && archetype != null) {
                    String archetypeId = personality.getArchetypeId();
                    String hobbyId = personality.getHobbyId();
                    for(int i = 0; i < possibleResponsesAmount; i++){
                        String key = "message.magic_realms.hobby." + hobbyId + "." + archetypeId + "." + i;
                        result.add(key);
                    }
                }
            }

            if (mercenary instanceof IExclusiveMercenary exclusive) {
                List<String> extra = exclusive.getExclusiveSpeechTranslationKeys();
                if (extra != null && !extra.isEmpty()) {
                    if(mercenary instanceof LilacEntity lilac && !lilac.level().isClientSide() && lilac.getSummoner() != null){
                        if(hasContractedAlianaNearby(lilac, lilac.level())){
                            result.addAll(extra);
                        }
                    } else if (mercenary instanceof JaraEntity jara && !jara.level().isClientSide() && jara.getSummoner() != null) {
                        if(hasContractedEdenNearby(jara, jara.level())){
                            result.addAll(extra);
                        }
                    } else {
                        result.addAll(extra);
                    }
                }
            }

            if(!result.isEmpty()){
                Random random = new Random();
                String chosenKey = result.get(random.nextInt(result.size()));
                return Component.translatable(chosenKey);
            }
        }
        return Component.empty();
    }

    private static boolean hasContractedAlianaNearby(LilacEntity entity, Level level) {
        double SEARCH_RADIUS = 20.0;
        AABB searchArea = new AABB(
                entity.getX() - SEARCH_RADIUS,
                entity.getY() - SEARCH_RADIUS,
                entity.getZ() - SEARCH_RADIUS,
                entity.getX() + SEARCH_RADIUS,
                entity.getY() + SEARCH_RADIUS,
                entity.getZ() + SEARCH_RADIUS
        );

        List<AlianaEntity> nearbyAliana = level.getEntitiesOfClass(
                AlianaEntity.class,
                searchArea,
                alianaEntity -> alianaEntity.getSummoner() != null && alianaEntity.getSummoner().is(entity.getSummoner())
        );

        return !nearbyAliana.isEmpty();
    }

    private static boolean hasContractedEdenNearby(JaraEntity entity, Level level) {
        double SEARCH_RADIUS = 20.0;
        AABB searchArea = new AABB(
                entity.getX() - SEARCH_RADIUS,
                entity.getY() - SEARCH_RADIUS,
                entity.getZ() - SEARCH_RADIUS,
                entity.getX() + SEARCH_RADIUS,
                entity.getY() + SEARCH_RADIUS,
                entity.getZ() + SEARCH_RADIUS
        );

        List<AceEntity> nearbyEden = level.getEntitiesOfClass(
                AceEntity.class,
                searchArea,
                aceEntity -> aceEntity.getSummoner() != null && aceEntity.getSummoner().is(entity.getSummoner())
        );

        return !nearbyEden.isEmpty();
    }
}
