package com.lucky7.mod.game.probability;

import com.lucky7.mod.game.WinCategory;

import java.util.random.RandomGenerator;

public record ProbabilityProfile(
    double jackpot,
    double bigWin,
    double mediumWin,
    double smallWin,
    double pair,
    double noWin
) {
    private static final double EPSILON = 1.0e-10;

    public ProbabilityProfile {
        if (!isUnit(jackpot)||!isUnit(bigWin)||!isUnit(mediumWin)||!isUnit(smallWin)||!isUnit(pair)||!isUnit(noWin)) {
            throw new IllegalArgumentException("Every probability must be finite and between zero and one");
        }
        if (Math.abs(jackpot+bigWin+mediumWin+smallWin+pair+noWin-1.0)>EPSILON) {
            throw new IllegalArgumentException("Probabilities must add up to one");
        }
    }

    public double anyWin() {
        return 1.0-noWin;
    }

    public double probability(WinCategory category) {
        return switch (category) {
            case JACKPOT -> jackpot;
            case BIG_WIN -> bigWin;
            case MEDIUM_WIN -> mediumWin;
            case SMALL_WIN -> smallWin;
            case PAIR -> pair;
            case NO_WIN -> noWin;
        };
    }

    public WinCategory choose(double roll) {
        double cumulative = jackpot;
        if (roll < cumulative) return WinCategory.JACKPOT;
        cumulative += bigWin;
        if (roll < cumulative) return WinCategory.BIG_WIN;
        cumulative += mediumWin;
        if (roll < cumulative) return WinCategory.MEDIUM_WIN;
        cumulative += smallWin;
        if (roll < cumulative) return WinCategory.SMALL_WIN;
        cumulative += pair;
        return roll < cumulative ? WinCategory.PAIR : WinCategory.NO_WIN;
    }

    public WinCategory choose(RandomGenerator random) {
        return choose(random.nextDouble());
    }

    private static boolean isUnit(double value) {
        return Double.isFinite(value)&&value>=0.0&&value<=1.0;
    }
}
