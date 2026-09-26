package net.alshanex.magic_realms.util.humans.combat;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.compat.GunCompat;
import net.neoforged.fml.ModList;

public final class OptionalCombatClasses {

    public static final String IRONS_ARTIFICE = "irons_artifice";

    private OptionalCombatClasses() {}

    public static void registerAll() {
        if (ModList.get().isLoaded(IRONS_ARTIFICE)) {
            try {
                GunCompat.register();
                MagicRealms.LOGGER.debug("Iron's Arms 'n Artifice detected - gunslinger class enabled");
            } catch (Throwable t) {
                MagicRealms.LOGGER.error("Failed to register gunslinger class — irons_artifice API mismatch?", t);
            }
        } else {
            MagicRealms.LOGGER.debug("Iron's Arms 'n Artifice not present - gunslinger class disabled");
        }
    }
}
