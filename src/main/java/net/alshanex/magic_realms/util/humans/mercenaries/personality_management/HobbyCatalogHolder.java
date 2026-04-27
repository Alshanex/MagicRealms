package net.alshanex.magic_realms.util.humans.mercenaries.personality_management;

public final class HobbyCatalogHolder {

    private HobbyCatalogHolder() {}

    private static volatile HobbyCatalog server = HobbyCatalog.EMPTY;
    private static volatile HobbyCatalog client = HobbyCatalog.EMPTY;

    public static HobbyCatalog server() {
        return server;
    }

    public static HobbyCatalog client() {
        return client;
    }

    public static void setServer(HobbyCatalog catalog) {
        server = catalog == null ? HobbyCatalog.EMPTY : catalog;
    }

    public static void setClient(HobbyCatalog catalog) {
        client = catalog == null ? HobbyCatalog.EMPTY : catalog;
    }

    public static HobbyCatalog get(boolean isClientSide) {
        return isClientSide ? client : server;
    }
}
