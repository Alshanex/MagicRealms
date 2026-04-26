package net.alshanex.magic_realms.util.humans.mercenaries.personality;

public final class ArchetypeCatalogHolder {

    private ArchetypeCatalogHolder() {}

    private static volatile ArchetypeCatalog server = ArchetypeCatalog.EMPTY;
    private static volatile ArchetypeCatalog client = ArchetypeCatalog.EMPTY;

    public static ArchetypeCatalog server() { return server; }
    public static ArchetypeCatalog client() { return client; }

    public static ArchetypeCatalog get(boolean isClientSide) {
        return isClientSide ? client : server;
    }

    public static void setServer(ArchetypeCatalog newCatalog) {
        server = newCatalog != null ? newCatalog : ArchetypeCatalog.EMPTY;
    }

    public static void setClient(ArchetypeCatalog newCatalog) {
        client = newCatalog != null ? newCatalog : ArchetypeCatalog.EMPTY;
    }
}
