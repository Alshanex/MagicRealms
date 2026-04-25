package net.alshanex.magic_realms.util;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory-wide checks for spell items, used to gate certain interactions.
 */
public final class SpellInventoryUtils {

    private SpellInventoryUtils() {}

    /**
     * Returns true if the player has any spell-container item carrying at least one Blood-school spell.
     */
    public static boolean playerHasBloodSpell(Player player) {
        if (player == null) return false;

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stackContainsBloodSpell(stack)) return true;
        }

        if (stackContainsBloodSpell(player.getOffhandItem())) return true;

        return false;
    }

    private static boolean stackContainsBloodSpell(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!ISpellContainer.isSpellContainer(stack)) return false;

        ISpellContainer container = ISpellContainer.get(stack);
        if (container == null || container.isEmpty()) return false;

        for (SpellSlot slot : container.getActiveSpells()) {
            if (slot == null) continue;
            AbstractSpell spell = slot.spellData().getSpell();
            if (spell == null) continue;
            if (spell.getSchoolType() == SchoolRegistry.BLOOD.get()) {
                return true;
            }
        }
        return false;
    }
}
