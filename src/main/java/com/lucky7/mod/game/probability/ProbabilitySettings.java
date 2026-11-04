package com.lucky7.mod.game.probability;

public record ProbabilitySettings(
    double winBase,
    double winPerValue,
    double winMax,
    double jackpotBase,
    double jackpotPerValue,
    double jackpotMax,
    double bigWeight,
    double mediumWeight,
    double smallWeight,
    double pairWeight
) {
    public static final ProbabilitySettings DEFAULTS=new ProbabilitySettings(
        0.06,0.0125,0.72,
        0.0,0.002,0.08,
        0.13,0.22,0.40,0.25
    );
}
