package net.alshanex.magic_realms.util.humans.combat;

import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.util.humans.mercenaries.EntitySnapshot;

public record ClassLoadout(boolean hasShield, boolean isArcher) {

    public static final ClassLoadout NONE = new ClassLoadout(false, false);

    public static ClassLoadout of(AbstractMercenaryEntity entity) {
        return entity == null ? NONE : new ClassLoadout(entity.hasShield(), entity.isArcher());
    }

    public static ClassLoadout of(EntitySnapshot snapshot) {
        return snapshot == null ? NONE : new ClassLoadout(snapshot.hasShield, snapshot.isArcher);
    }
}
