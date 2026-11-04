package com.lucky7.mod.game.probability;

public final class ProbabilityCalculator {
    private ProbabilityCalculator() {
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public static ProbabilityProfile calculate(int stakeValue, ProbabilitySettings settings) {
        int safeValue = Math.max(0, stakeValue);
        double anyWin = clamp(settings.winBase() + safeValue * settings.winPerValue(), settings.winBase(), settings.winMax());
        double jackpot = clamp(settings.jackpotBase() + safeValue * settings.jackpotPerValue(), 0.0, Math.min(settings.jackpotMax(), anyWin));

        double distributable = Math.max(0.0, anyWin - jackpot);
        double wBig = Math.max(0.0, settings.bigWeight());
        double wMed = Math.max(0.0, settings.mediumWeight());
        double wSmall = Math.max(0.0, settings.smallWeight());
        double wPair = Math.max(0.0, settings.pairWeight());
        double weightSum = wBig + wMed + wSmall + wPair;
        if (weightSum <= 0.0) {
            wBig = 0.10; wMed = 0.20; wSmall = 0.40; wPair = 0.30;
            weightSum = 1.0;
        }

        double big = distributable * (wBig / weightSum);
        double medium = distributable * (wMed / weightSum);
        double small = distributable * (wSmall / weightSum);
        double pair = distributable * (wPair / weightSum);
        double noWin = Math.max(0.0, 1.0 - anyWin);

        // Precision normalization to exactly 1.0
        double sum = jackpot + big + medium + small + pair + noWin;
        if (sum > 0.0 && Math.abs(sum - 1.0) > 1.0e-14) {
            jackpot /= sum;
            big /= sum;
            medium /= sum;
            small /= sum;
            pair /= sum;
            noWin = 1.0 - (jackpot + big + medium + small + pair);
        }

        return new ProbabilityProfile(jackpot, big, medium, small, pair, noWin);
    }
}
