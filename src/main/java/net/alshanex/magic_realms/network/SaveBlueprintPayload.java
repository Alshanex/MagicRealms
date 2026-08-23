package net.alshanex.magic_realms.network;

import io.netty.buffer.ByteBuf;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.util.MRUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SaveBlueprintPayload(boolean isMainHand, CompoundTag assemblyData, String customName) implements CustomPacketPayload {

    public static final Type<SaveBlueprintPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MagicRealms.MODID, "save_blueprint"));

    public static final StreamCodec<ByteBuf, SaveBlueprintPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SaveBlueprintPayload::isMainHand,
                    ByteBufCodecs.COMPOUND_TAG, SaveBlueprintPayload::assemblyData,
                    ByteBufCodecs.STRING_UTF8, SaveBlueprintPayload::customName,
                    SaveBlueprintPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveBlueprintPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            MRUtils.handleAssemblyBlueprintPacket(context.player(), payload.isMainHand(), payload.assemblyData(), payload.customName());
        });
    }
}
