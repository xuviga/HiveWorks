package com.xuvigan.hiveworks.hive;

/**
 * Frame definitions. Values mirror frames.yml and will be loaded from it in 0.1.1.
 */
public enum FrameType {

    WAX("Восковая рамка", 32, 1, 0.00, 0.10, 0.00, 0.00),
    HONEY("Медовая рамка", 64, 0, 0.25, 0.10, 0.00, 0.00),
    ROYAL("Маточная рамка", 16, 0, 0.00, 0.30, 0.00, 0.00),
    PROPOLIS("Прополисная рамка", 48, 0, 0.00, 0.10, 0.00, 0.50),
    AMETHYST("Аметистовая рамка", 128, 0, 0.10, 0.40, 0.08, 0.00);

    private final String displayName;
    private final int maxUses;
    private final int maxHoneyBonus;
    private final double speedBonus;
    private final double qualityWeight;
    private final double qualityBonus;
    private final double stingReduction;

    FrameType(String displayName,
              int maxUses,
              int maxHoneyBonus,
              double speedBonus,
              double qualityWeight,
              double qualityBonus,
              double stingReduction) {
        this.displayName = displayName;
        this.maxUses = maxUses;
        this.maxHoneyBonus = maxHoneyBonus;
        this.speedBonus = speedBonus;
        this.qualityWeight = qualityWeight;
        this.qualityBonus = qualityBonus;
        this.stingReduction = stingReduction;
    }

    public String displayName() {
        return displayName;
    }

    public int maxUses() {
        return maxUses;
    }

    public int maxHoneyBonus() {
        return maxHoneyBonus;
    }

    public double speedBonus() {
        return speedBonus;
    }

    public double qualityWeight() {
        return qualityWeight;
    }

    public double qualityBonus() {
        return qualityBonus;
    }

    public double stingReduction() {
        return stingReduction;
    }
}
