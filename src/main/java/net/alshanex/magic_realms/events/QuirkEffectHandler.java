package net.alshanex.magic_realms.events;

import net.alshanex.magic_realms.data.PersonalityData;
import net.alshanex.magic_realms.entity.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.ModTags;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.Quirk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

public class QuirkEffectHandler {

    public static void tickQuirks(AbstractMercenaryEntity entity) {
        // Run every 20 ticks (1 second) server-side
        if (entity.level().isClientSide() || entity.tickCount % 20 != 0) return;

        PersonalityData personality = entity.getData(MRDataAttachments.PERSONALITY);
        if (personality == null) return;

        Set<Quirk> quirks = personality.getQuirks();
        if (quirks == null || quirks.isEmpty()) return;

        // Base amplifier is -1 (Meaning No Effect).
        // A level 1 effect in Minecraft uses Amplifier 0, Level 2 uses Amplifier 1.
        // By adding exactly the levels requested, they naturally stack together perfectly.
        int speedAmp = -1;
        int resistanceAmp = -1;
        int absorptionAmp = -1;
        int slownessAmp = -1;
        int fatigueAmp = -1;
        int weaknessAmp = -1;

        Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        // AFRAID_OF_THE_DARK
        if (quirks.contains(Quirk.AFRAID_OF_THE_DARK) && level.getMaxLocalRawBrightness(pos) <= 4) {
            slownessAmp += 2;
            fatigueAmp += 1;
            weaknessAmp += 1;
        }

        // ANIMAL_FRIEND
        if (quirks.contains(Quirk.ANIMAL_FRIEND)) {
            List<Animal> animals = level.getEntitiesOfClass(Animal.class, entity.getBoundingBox().inflate(8.0D));
            if (!animals.isEmpty()) {
                speedAmp += 1;
                absorptionAmp += 3; // +3 results in Amplifier 2 (Absorption III)
            }
        }

        // CLAUSTROPHOBIC
        if (quirks.contains(Quirk.CLAUSTROPHOBIC) && isNarrowPlace(level, pos)) {
            slownessAmp += 2;
            fatigueAmp += 1;
            weaknessAmp += 1;
        }

        // NIGHT_OWL
        if (quirks.contains(Quirk.NIGHT_OWL) && level.isNight()) {
            speedAmp += 1;
            resistanceAmp += 1;
        }

        // EARLY_RISER
        if (quirks.contains(Quirk.EARLY_RISER) && level.isDay()) {
            speedAmp += 1;
            resistanceAmp += 1;
        }

        // HEAT_INTOLERANT
        if (quirks.contains(Quirk.HEAT_INTOLERANT) && level.getBiome(pos).is(ModTags.HOT_BIOMES)) {
            slownessAmp += 2;
            fatigueAmp += 1;
            weaknessAmp += 1;
        }

        // COLD_INTOLERANT
        if (quirks.contains(Quirk.COLD_INTOLERANT) && level.getBiome(pos).is(ModTags.COLD_BIOMES)) {
            slownessAmp += 2;
            fatigueAmp += 1;
            weaknessAmp += 1;
        }

        // HEIGHT_SCARED
        if (quirks.contains(Quirk.HEIGHT_SCARED) && entity.getY() > 100) {
            slownessAmp += 2;
            fatigueAmp += 1;
            weaknessAmp += 1;
        }

        // BOOKWORM & GLUTTON Check (Inventory scan)
        boolean hasBook = false;
        boolean hasFood = false;
        if (quirks.contains(Quirk.BOOKWORM) || quirks.contains(Quirk.GLUTTON)) {
            for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
                ItemStack stack = entity.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    if (stack.is(ModTags.BOOKS)) hasBook = true;
                    // In 1.21.1, check DataComponents for food properties
                    if (stack.has(DataComponents.FOOD)) hasFood = true;
                }
            }
        }

        if (quirks.contains(Quirk.BOOKWORM) && hasBook) {
            speedAmp += 1;
            resistanceAmp += 1;
        }

        if (quirks.contains(Quirk.GLUTTON) && hasFood) {
            speedAmp += 1;
            resistanceAmp += 1;
        }

        // Apply Accumulated Effects (duration 40 ticks seamlessly overlaps the 20-tick check cycle)
        if (speedAmp >= 0) entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, speedAmp, false, false));
        if (resistanceAmp >= 0) entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, resistanceAmp, false, false));
        if (absorptionAmp >= 0) entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, absorptionAmp, false, false));
        if (slownessAmp >= 0) entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, slownessAmp, false, false));
        if (fatigueAmp >= 0) entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, fatigueAmp, false, false));
        if (weaknessAmp >= 0) entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, weaknessAmp, false, false));
    }

    private static boolean isNarrowPlace(Level level, BlockPos pos) {
        // If 3 or more surrounding blocks (walls + roof) are solid, consider it a narrow space
        int solidCount = 0;
        if (level.getBlockState(pos.north()).canOcclude()) solidCount++;
        if (level.getBlockState(pos.south()).canOcclude()) solidCount++;
        if (level.getBlockState(pos.east()).canOcclude()) solidCount++;
        if (level.getBlockState(pos.west()).canOcclude()) solidCount++;
        if (level.getBlockState(pos.above(2)).canOcclude()) solidCount++;

        return solidCount >= 3;
    }
}
