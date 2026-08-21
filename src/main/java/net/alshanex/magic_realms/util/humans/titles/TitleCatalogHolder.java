package net.alshanex.magic_realms.util.humans.titles;

public final class TitleCatalogHolder {

    private TitleCatalogHolder() {}

    private static volatile TitleCatalog server = TitleCatalog.EMPTY;
    private static volatile TitleCatalog client = TitleCatalog.EMPTY;

    public static TitleCatalog server() { return server; }
    public static TitleCatalog client() { return client; }

    public static TitleCatalog get(boolean isClientSide) {
        return isClientSide ? client : server;
    }

    public static void setServer(TitleCatalog newCatalog) {
        server = newCatalog != null ? newCatalog : TitleCatalog.EMPTY;
    }

    public static void setClient(TitleCatalog newCatalog) {
        client = newCatalog != null ? newCatalog : TitleCatalog.EMPTY;
    }
}
