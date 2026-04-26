package net.alshanex.magic_realms.entity;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

public interface IExclusiveMercenary {
    String getExclusiveMercenaryName();
    String getExclusiveMercenaryPresentationMessage();

    default List<String> getExclusiveSpeechTranslationKeys() {
        return Collections.emptyList();
    }

    default ItemStack getDefaultVisualArmor(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }
}
