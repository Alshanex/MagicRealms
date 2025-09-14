package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.MRUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class RequestEntityLevelPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestEntityLevelPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "request_entity_level"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestEntityLevelPacket> STREAM_CODEC =
            CustomPacketPayload.codec(RequestEntityLevelPacket::write, RequestEntityLevelPacket::new);

    private final UUID entityUUID;

    public RequestEntityLevelPacket(UUID entityUUID) {
        this.entityUUID = entityUUID;
    }

    public RequestEntityLevelPacket(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
    }

    public static void handle(RequestEntityLevelPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.requestEntityLevel(context.player(), packet.entityUUID);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
