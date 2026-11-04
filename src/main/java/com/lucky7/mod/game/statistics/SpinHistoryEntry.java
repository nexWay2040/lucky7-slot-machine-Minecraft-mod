package com.lucky7.mod.game.statistics;

import com.lucky7.mod.game.WinCategory;
import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public record SpinHistoryEntry(
    long timestamp,
    UUID playerId,
    String playerName,
    int stakeValue,
    String result,
    WinCategory category,
    int rewardValue
) {
    public NbtCompound toNbt() {
        NbtCompound nbt=new NbtCompound();
        nbt.putLong("timestamp",timestamp);
        nbt.putString("playerId",playerId.toString());
        nbt.putString("playerName",playerName);
        nbt.putInt("stakeValue",stakeValue);
        nbt.putString("result",result);
        nbt.putString("category",category.name());
        nbt.putInt("rewardValue",rewardValue);
        return nbt;
    }

    public static SpinHistoryEntry fromNbt(NbtCompound nbt) {
        UUID playerId;
        WinCategory category;
        try { playerId=UUID.fromString(nbt.getString("playerId")); }
        catch (IllegalArgumentException ignored) { playerId=new UUID(0,0); }
        try { category=WinCategory.valueOf(nbt.getString("category")); }
        catch (IllegalArgumentException ignored) { category=WinCategory.NO_WIN; }
        return new SpinHistoryEntry(
            nbt.getLong("timestamp"),playerId,nbt.getString("playerName"),nbt.getInt("stakeValue"),
            nbt.getString("result"),category,nbt.getInt("rewardValue")
        );
    }
}
