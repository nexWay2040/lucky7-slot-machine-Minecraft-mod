package com.lucky7.mod.network;

import com.lucky7.mod.Lucky7Mod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record Lucky7ActionPayload(BlockPos pos, int action) implements CustomPacketPayload {
    public static final Type<Lucky7ActionPayload> TYPE = new Type<>(Lucky7Mod.id("action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Lucky7ActionPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, Lucky7ActionPayload::pos,
        ByteBufCodecs.VAR_INT, Lucky7ActionPayload::action,
        Lucky7ActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
