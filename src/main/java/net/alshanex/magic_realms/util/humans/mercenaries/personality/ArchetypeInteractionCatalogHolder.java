package net.alshanex.magic_realms.util.humans.mercenaries.personality;

public final class ArchetypeInteractionCatalogHolder {

    private ArchetypeInteractionCatalogHolder() {}

    private static volatile ArchetypeInteractionCatalog server = ArchetypeInteractionCatalog.EMPTY;
    private static volatile ArchetypeInteractionCatalog client = ArchetypeInteractionCatalog.EMPTY;

    public static ArchetypeInteractionCatalog server() { return server; }
    public static ArchetypeInteractionCatalog client() { return client; }

    public static ArchetypeInteractionCatalog get(boolean isClientSide) {
        return isClientSide ? client : server;
    }

    public static void setServer(ArchetypeInteractionCatalog newCatalog) {
        server = newCatalog != null ? newCatalog : ArchetypeInteractionCatalog.EMPTY;
    }

    public static void setClient(ArchetypeInteractionCatalog newCatalog) {
        client = newCatalog != null ? newCatalog : ArchetypeInteractionCatalog.EMPTY;
    }
}
