package com.lucky7.mod.game.statistics;

import com.lucky7.mod.game.WinCategory;
import net.minecraft.nbt.CompoundTag;

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
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("timestamp", timestamp);
        nbt.putString("playerId", playerId.toString());
        nbt.putString("playerName", playerName);
        nbt.putInt("stakeValue", stakeValue);
        nbt.putString("result", result);
        nbt.putString("category", category.name());
        nbt.putInt("rewardValue", rewardValue);
        return nbt;
    }

    public static SpinHistoryEntry fromNbt(CompoundTag nbt) {
        UUID playerId;
        WinCategory category;
        try {
            playerId = UUID.fromString(nbt.getStringOr("playerId", ""));
        } catch (IllegalArgumentException ignored) {
            playerId = new UUID(0, 0);
        }
        try {
            category = WinCategory.valueOf(nbt.getStringOr("category", "NO_WIN"));
        } catch (IllegalArgumentException ignored) {
            category = WinCategory.NO_WIN;
        }
        return new SpinHistoryEntry(
            nbt.getLongOr("timestamp", 0L),
            playerId,
            nbt.getStringOr("playerName", ""),
            nbt.getIntOr("stakeValue", 0),
            nbt.getStringOr("result", ""),
            category,
            nbt.getIntOr("rewardValue", 0)
        );
    }
}
