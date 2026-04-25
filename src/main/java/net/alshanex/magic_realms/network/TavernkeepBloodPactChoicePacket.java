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

public class TavernkeepBloodPactChoicePacket implements CustomPacketPayload {
    public static final Type<TavernkeepBloodPactChoicePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "tavernkeep_blood_pact_choice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TavernkeepBloodPactChoicePacket> STREAM_CODEC =
            CustomPacketPayload.codec(TavernkeepBloodPactChoicePacket::write, TavernkeepBloodPactChoicePacket::new);

    private final UUID tavernkeepUUID;
    private final MRUtils.Choice choice;

    public TavernkeepBloodPactChoicePacket(UUID tavernkeepUUID,  MRUtils.Choice choice) {
        this.tavernkeepUUID = tavernkeepUUID;
        this.choice = choice;
    }

    public TavernkeepBloodPactChoicePacket(FriendlyByteBuf buf) {
        this.tavernkeepUUID = buf.readUUID();
        this.choice = buf.readEnum( MRUtils.Choice.class);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(tavernkeepUUID);
        buf.writeEnum(choice);
    }

    public static void handle(TavernkeepBloodPactChoicePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.handleTavernkeeperChoicePacket(context.player(), packet.tavernkeepUUID, packet.choice);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
