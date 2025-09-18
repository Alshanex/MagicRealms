package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.MRUtils;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.alshanex.magic_realms.util.humans.Gender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class RequestTextureGenerationPacket implements CustomPacketPayload {
    public static final Type<RequestTextureGenerationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "request_texture_generation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestTextureGenerationPacket> STREAM_CODEC =
            CustomPacketPayload.codec(RequestTextureGenerationPacket::write, RequestTextureGenerationPacket::new);

    private final UUID entityUUID;
    private final int genderOrdinal;
    private final int entityClassOrdinal;

    public RequestTextureGenerationPacket(UUID entityUUID, Gender gender, EntityClass entityClass) {
        this.entityUUID = entityUUID;
        this.genderOrdinal = gender.ordinal();
        this.entityClassOrdinal = entityClass.ordinal();
    }

    public RequestTextureGenerationPacket(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
        this.genderOrdinal = buf.readVarInt();
        this.entityClassOrdinal = buf.readVarInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        buf.writeVarInt(genderOrdinal);
        buf.writeVarInt(entityClassOrdinal);
    }

    public static void handle(RequestTextureGenerationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                MRUtils.handleClientSideTextureGeneration(packet.entityUUID, packet.genderOrdinal, packet.entityClassOrdinal);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
