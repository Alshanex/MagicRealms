package net.alshanex.magic_realms.util.humans;

public enum EntityClass {
    MAGE("mage"),
    ROGUE("rogue"),
    WARRIOR("warrior");

    private final String name;

    EntityClass(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
