package com.lucky7.mod.network;

import com.lucky7.mod.Lucky7Mod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record Lucky7ActionPayload(BlockPos pos, int action) implements CustomPayload {
    public static final Id<Lucky7ActionPayload> ID = new Id<>(Lucky7Mod.id("action"));
    public static final PacketCodec<RegistryByteBuf, Lucky7ActionPayload> CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC, Lucky7ActionPayload::pos,
        PacketCodecs.VAR_INT, Lucky7ActionPayload::action,
        Lucky7ActionPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
