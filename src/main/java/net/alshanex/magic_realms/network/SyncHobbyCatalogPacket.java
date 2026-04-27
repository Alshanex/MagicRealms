package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.Hobby;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.HobbyCatalog;
import net.alshanex.magic_realms.util.humans.mercenaries.personality_management.HobbyCatalogHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Ships the hobby catalog (datapack-loaded definitions) from server to client on datapack sync.
 */
public class SyncHobbyCatalogPacket implements CustomPacketPayload {

    public static final Type<SyncHobbyCatalogPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "sync_hobby_catalog"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncHobbyCatalogPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncHobbyCatalogPacket::write, SyncHobbyCatalogPacket::new);

    private final List<Hobby> hobbies;

    public SyncHobbyCatalogPacket(HobbyCatalog catalog) {
        this.hobbies = catalog.all();
    }

    public SyncHobbyCatalogPacket(List<Hobby> hobbies) {
        this.hobbies = hobbies;
    }

    public SyncHobbyCatalogPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Hobby> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(Hobby.readFromBuf(buf));
        this.hobbies = list;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(hobbies.size());
        for (Hobby h : hobbies) Hobby.writeToBuf(buf, h);
    }

    public static void handle(SyncHobbyCatalogPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> HobbyCatalogHolder.setClient(new HobbyCatalog(packet.hobbies)));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
