package net.alshanex.magic_realms.network;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.SummoningUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SummonAlliesPackage implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SummonAlliesPackage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "summon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SummonAlliesPackage> STREAM_CODEC = CustomPacketPayload.codec(SummonAlliesPackage::write, SummonAlliesPackage::new);

    public SummonAlliesPackage() {
    }

    public SummonAlliesPackage(FriendlyByteBuf buf) {
    }

    public void write(FriendlyByteBuf buf) {
    }

    public static void handle(SummonAlliesPackage packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                SummoningUtils.handleAlliesSummoning(serverPlayer);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
