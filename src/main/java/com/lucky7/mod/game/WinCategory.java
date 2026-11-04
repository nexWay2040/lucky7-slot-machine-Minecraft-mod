package com.lucky7.mod.game;

public enum WinCategory {
    JACKPOT,
    BIG_WIN,
    MEDIUM_WIN,
    SMALL_WIN,
    PAIR,
    NO_WIN;

    public boolean isWin() {
        return this != NO_WIN;
    }
}
