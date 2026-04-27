package net.alshanex.magic_realms.util.humans.mercenaries.personality_management;

public final class FixedPersonalityCatalogHolder {

    private FixedPersonalityCatalogHolder() {}

    private static volatile FixedPersonalityCatalog server = FixedPersonalityCatalog.EMPTY;
    private static volatile FixedPersonalityCatalog client = FixedPersonalityCatalog.EMPTY;

    public static FixedPersonalityCatalog server() { return server; }
    public static FixedPersonalityCatalog client() { return client; }

    public static FixedPersonalityCatalog get(boolean isClientSide) {
        return isClientSide ? client : server;
    }

    public static void setServer(FixedPersonalityCatalog newCatalog) {
        server = newCatalog != null ? newCatalog : FixedPersonalityCatalog.EMPTY;
    }

    public static void setClient(FixedPersonalityCatalog newCatalog) {
        client = newCatalog != null ? newCatalog : FixedPersonalityCatalog.EMPTY;
    }
}
