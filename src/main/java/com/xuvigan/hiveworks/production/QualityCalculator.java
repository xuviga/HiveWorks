package com.xuvigan.hiveworks.production;

/**
 * Pure quality formula: 0.60 flowers + 0.25 frames + 0.15 purity (+ bonuses).
 */
public final class QualityCalculator {

    private final double flowerWeight;
    private final double frameWeight;
    private final double purityWeight;
    private final double goodThreshold;
    private final double eliteThreshold;
    private final double legendaryThreshold;

    public QualityCalculator(double flowerWeight,
                             double frameWeight,
                             double purityWeight,
                             double goodThreshold,
                             double eliteThreshold,
                             double legendaryThreshold) {
        this.flowerWeight = flowerWeight;
        this.frameWeight = frameWeight;
        this.purityWeight = purityWeight;
        this.goodThreshold = goodThreshold;
        this.eliteThreshold = eliteThreshold;
        this.legendaryThreshold = legendaryThreshold;
    }

    public static QualityCalculator defaults() {
        return new QualityCalculator(0.60, 0.25, 0.15, 0.35, 0.60, 0.85);
    }

    public double score(int flowerSpecies, boolean rareFlowers, double frameScore, int purity, double extraBonus) {
        double flowerScore = clamp01((flowerSpecies + (rareFlowers ? 0.5 : 0.0)) / 5.0);
        double frames = clamp01(frameScore);
        double purityScore = clamp01(purity / 100.0);
        double total = flowerWeight * flowerScore
                + frameWeight * frames
                + purityWeight * purityScore
                + Math.max(0.0, extraBonus);
        return clamp01(total);
    }

    public Quality qualityOf(double score) {
        if (score >= legendaryThreshold) {
            return Quality.LEGENDARY;
        }
        if (score >= eliteThreshold) {
            return Quality.ELITE;
        }
        if (score >= goodThreshold) {
            return Quality.GOOD;
        }
        return Quality.ORDINARY;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
