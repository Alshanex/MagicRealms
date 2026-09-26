package net.alshanex.magic_realms.util.humans.mercenaries.skins_management;

import com.mojang.serialization.Codec;
import net.alshanex.magic_realms.util.humans.combat.CombatClass;

import javax.annotation.Nullable;
import java.util.Locale;

public record ClassFilter(String name) {

    public static final ClassFilter ANY = new ClassFilter("any");
    public static final ClassFilter COMMON = new ClassFilter("common");

    public static final Codec<ClassFilter> CODEC = Codec.STRING.xmap(
            s -> new ClassFilter(s == null ? "any" : s.trim().toLowerCase(Locale.ROOT)),
            ClassFilter::name);

    public ClassFilter {
        name = name == null ? "any" : name.trim().toLowerCase(Locale.ROOT);
    }

    public boolean matches(@Nullable CombatClass combatClass) {
        if (isWildcard()) return true;
        if (combatClass == null) return false;
        for (String category : combatClass.skinCategories()) {
            if (name.equalsIgnoreCase(category)) return true;
        }
        return false;
    }

    public boolean isWildcard() {
        return name.equals("any") || name.equals("common");
    }

    public boolean isCommonFallback() {
        return name.equals("common");
    }
}
