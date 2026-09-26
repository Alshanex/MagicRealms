package net.alshanex.magic_realms.compat;

import net.alshanex.magic_realms.util.humans.combat.CombatClasses;

public final class GunCompat {

    private GunCompat() {}

    public static void register() {
        CombatClasses.register(new GunslingerClass());
    }
}
