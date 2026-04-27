package net.alshanex.magic_realms.util.humans.bandits;

public final class BanditProfileCatalogHolder {

    private BanditProfileCatalogHolder() {}

    private static volatile BanditProfileCatalog server = BanditProfileCatalog.EMPTY;
    private static volatile BanditProfileCatalog client = BanditProfileCatalog.EMPTY;

    public static BanditProfileCatalog server() { return server; }
    public static BanditProfileCatalog client() { return client; }

    public static BanditProfileCatalog get(boolean isClientSide) {
        return isClientSide ? client : server;
    }

    public static void setServer(BanditProfileCatalog newCatalog) {
        server = newCatalog != null ? newCatalog : BanditProfileCatalog.EMPTY;
    }

    public static void setClient(BanditProfileCatalog newCatalog) {
        client = newCatalog != null ? newCatalog : BanditProfileCatalog.EMPTY;
    }
}
