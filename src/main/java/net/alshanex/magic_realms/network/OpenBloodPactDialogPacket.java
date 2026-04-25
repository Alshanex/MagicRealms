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

public class OpenBloodPactDialogPacket implements CustomPacketPayload {
    public static final Type<OpenBloodPactDialogPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "open_blood_pact_dialog"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBloodPactDialogPacket> STREAM_CODEC =
            CustomPacketPayload.codec(OpenBloodPactDialogPacket::write, OpenBloodPactDialogPacket::new);

    private final UUID tavernkeepUUID;

    public OpenBloodPactDialogPacket(UUID tavernkeepUUID) {
        this.tavernkeepUUID = tavernkeepUUID;
    }

    public OpenBloodPactDialogPacket(FriendlyByteBuf buf) {
        this.tavernkeepUUID = buf.readUUID();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(tavernkeepUUID);
    }

    public static void handle(OpenBloodPactDialogPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.handleTavernkeeperScreenPacket(packet.tavernkeepUUID);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
