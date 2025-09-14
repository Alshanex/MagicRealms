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

public class UpdateEntityNamePacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdateEntityNamePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "update_entity_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateEntityNamePacket> STREAM_CODEC =
            CustomPacketPayload.codec(UpdateEntityNamePacket::write, UpdateEntityNamePacket::new);

    private final UUID entityUUID;
    private final String entityName;

    public UpdateEntityNamePacket(UUID entityUUID, String entityName) {
        this.entityUUID = entityUUID;
        this.entityName = entityName;
    }

    public UpdateEntityNamePacket(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
        this.entityName = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        buf.writeUtf(entityName);
    }

    public static void handle(UpdateEntityNamePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.handleHumanNameUpdate(context.player(), packet.entityUUID, packet.entityName);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
