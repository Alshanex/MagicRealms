package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.bandits.BanditProfile;
import net.alshanex.magic_realms.util.humans.bandits.BanditProfileCatalog;
import net.alshanex.magic_realms.util.humans.bandits.BanditProfileCatalogHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Ships the bandit profile catalog (datapack-loaded definitions) from server to client on datapack sync.
 */
public class SyncBanditProfileCatalogPacket implements CustomPacketPayload {

    public static final Type<SyncBanditProfileCatalogPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_bandit_profile_catalog"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBanditProfileCatalogPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncBanditProfileCatalogPacket::write, SyncBanditProfileCatalogPacket::new);

    private final List<BanditProfile> profiles;

    public SyncBanditProfileCatalogPacket(BanditProfileCatalog catalog) {
        this.profiles = catalog.all();
    }

    public SyncBanditProfileCatalogPacket(List<BanditProfile> profiles) {
        this.profiles = profiles;
    }

    public SyncBanditProfileCatalogPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<BanditProfile> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(BanditProfile.readFromBuf(buf));
        this.profiles = list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(profiles.size());
        for (BanditProfile p : profiles) BanditProfile.writeToBuf(buf, p);
    }

    public static void handle(SyncBanditProfileCatalogPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> BanditProfileCatalogHolder.setClient(new BanditProfileCatalog(packet.profiles)));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
