package net.alshanex.magic_realms.util.humans.mercenaries.skins_management;

public final class SkinCatalogHolder {
    private static volatile SkinCatalog SERVER = SkinCatalog.EMPTY;
    private static volatile SkinCatalog CLIENT = SkinCatalog.EMPTY;

    private SkinCatalogHolder() {}

    public static SkinCatalog server() { return SERVER; }
    public static SkinCatalog client() { return CLIENT; }

    public static void setServer(SkinCatalog catalog) {
        SERVER = catalog == null ? SkinCatalog.EMPTY : catalog;
    }

    public static void setClient(SkinCatalog catalog) {
        CLIENT = catalog == null ? SkinCatalog.EMPTY : catalog;
    }

    /**
     * Context-aware lookup: use server catalog on server thread, client catalog otherwise.
     * Useful in shared code paths (e.g. virtual entities in GUIs).
     */
    public static SkinCatalog current(boolean isClientSide) {
        return isClientSide ? CLIENT : SERVER;
    }
}
