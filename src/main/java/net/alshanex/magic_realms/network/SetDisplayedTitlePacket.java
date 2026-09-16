package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.ContractData;
import net.alshanex.magic_realms.entity.humans.AbstractMercenaryEntity;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.titles.TitleManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Sent when the contractor picks which of a mercenary's earned titles should show on its nameplate.
 * An empty title string clears the choice, falling back to the automatic highest-priority pick.
 *
 * <p>The server re-validates ownership and ownership of the title - the client is never trusted here.
 */
public class SetDisplayedTitlePacket implements CustomPacketPayload {

    public static final Type<SetDisplayedTitlePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "set_displayed_title"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetDisplayedTitlePacket> STREAM_CODEC =
            CustomPacketPayload.codec(SetDisplayedTitlePacket::write, SetDisplayedTitlePacket::new);

    private final UUID entityUUID;
    private final String titleId;

    public SetDisplayedTitlePacket(UUID entityUUID, ResourceLocation titleId) {
        this.entityUUID = entityUUID;
        this.titleId = titleId == null ? "" : titleId.toString();
    }

    public SetDisplayedTitlePacket(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
        this.titleId = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        buf.writeUtf(titleId);
    }

    public static void handle(SetDisplayedTitlePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;

            ServerLevel level = serverPlayer.serverLevel();
            Entity entity = level.getEntity(packet.entityUUID);
            if (!(entity instanceof AbstractMercenaryEntity mercenary)) return;

            // Only the current contractor may change the displayed title.
            ContractData contract = mercenary.getData(MRDataAttachments.CONTRACT_DATA);
            if (contract == null || !contract.hasActiveContract(level)) return;
            UUID contractor = contract.getContractorUUID();
            if (contractor == null || !contractor.equals(serverPlayer.getUUID())) return;

            ResourceLocation id = packet.titleId.isEmpty() ? null : ResourceLocation.tryParse(packet.titleId);

            // setDisplayedTitle already refuses ids the mercenary hasn't earned.
            TitleManager.setDisplayedTitle(mercenary, id);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
