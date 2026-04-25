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

public class TogglePatrolModePacket implements CustomPacketPayload {
    public static final Type<TogglePatrolModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "toggle_patrol_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TogglePatrolModePacket> STREAM_CODEC =
            CustomPacketPayload.codec(TogglePatrolModePacket::write, TogglePatrolModePacket::new);

    private final UUID entityUUID;

    public TogglePatrolModePacket(UUID entityUUID) {
        this.entityUUID = entityUUID;
    }

    public TogglePatrolModePacket(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
    }

    public static void handle(TogglePatrolModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.handlePatrolModePacket(context.player(), packet.entityUUID);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
