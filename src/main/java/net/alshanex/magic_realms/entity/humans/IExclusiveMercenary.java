package net.alshanex.magic_realms.entity.humans;

import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface IExclusiveMercenary {
    String getExclusiveMercenaryName();
    String getExclusiveMercenaryPresentationMessage();

    default List<String> getExclusiveSpeechTranslationKeys() {
        return Collections.emptyList();
    }

    default ItemStack getDefaultVisualArmor(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    Map<EquipmentSlot, Boolean> getVisualArmorStateCache();

    default LivingEntity getAsLivingEntity() {
        return (LivingEntity) this;
    }

    default void tickVisualArmor() {
        LivingEntity entity = getAsLivingEntity();

        if (!entity.level().isClientSide() && entity.tickCount % 60 == 0) {
            Map<EquipmentSlot, Boolean> cache = getVisualArmorStateCache();

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.isArmor()) {
                    Boolean wasEmpty = cache.get(slot);
                    boolean isCurrentlyEmpty = entity.getItemBySlot(slot).isEmpty();

                    if (wasEmpty == null || wasEmpty != isCurrentlyEmpty) {
                        if (isCurrentlyEmpty) {
                            addVisualArmorAttributes(slot);
                        } else {
                            removeVisualArmorAttributes(slot);
                        }
                        cache.put(slot, isCurrentlyEmpty);
                    }
                }
            }
        }
    }

    default void addVisualArmorAttributes(EquipmentSlot slot) {
        LivingEntity entity = getAsLivingEntity();
        ItemStack visualArmor = this.getDefaultVisualArmor(slot);
        if (visualArmor.isEmpty()) return;

        ItemAttributeModifiers modifiers = visualArmor.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        if (modifiers.modifiers().isEmpty()) {
            modifiers = visualArmor.getItem().getDefaultAttributeModifiers();
        }

        modifiers.modifiers().forEach(entry -> {
            if (entry.slot().test(slot)) {
                AttributeInstance instance = entity.getAttribute(entry.attribute());
                if (instance != null) {
                    ResourceLocation customId = ResourceLocation.fromNamespaceAndPath(
                            MagicRealms.MODID, "visual_" + slot.getName() + "_" + entry.modifier().id().getPath());

                    instance.removeModifier(customId);

                    AttributeModifier customMod = new AttributeModifier(customId, entry.modifier().amount(), entry.modifier().operation());
                    instance.addTransientModifier(customMod);
                }
            }
        });
    }

    default void removeVisualArmorAttributes(EquipmentSlot slot) {
        LivingEntity entity = getAsLivingEntity();
        ItemStack visualArmor = this.getDefaultVisualArmor(slot);
        if (visualArmor.isEmpty()) return;

        ItemAttributeModifiers modifiers = visualArmor.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        if (modifiers.modifiers().isEmpty()) {
            modifiers = visualArmor.getItem().getDefaultAttributeModifiers();
        }

        modifiers.modifiers().forEach(entry -> {
            if (entry.slot().test(slot)) {
                AttributeInstance instance = entity.getAttribute(entry.attribute());
                if (instance != null) {
                    ResourceLocation customId = ResourceLocation.fromNamespaceAndPath(
                            MagicRealms.MODID, "visual_" + slot.getName() + "_" + entry.modifier().id().getPath());

                    instance.removeModifier(customId);
                }
            }
        });
    }
}
