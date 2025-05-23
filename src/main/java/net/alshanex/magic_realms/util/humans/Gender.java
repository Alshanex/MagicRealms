package net.alshanex.magic_realms.util.humans;

public enum Gender {
    MALE("male"),
    FEMALE("female");

    private final String name;

    Gender(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
