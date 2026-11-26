package com.xuvigan.hiveworks.hive;

/**
 * Frame definitions. Values mirror frames.yml and will be loaded from it in 0.2.
 */
public enum FrameType {

    WAX("Восковая рамка", 1101, 32, 1, 0.00, 0.10, 0.00, 0.00),
    HONEY("Медовая рамка", 1102, 64, 0, 0.25, 0.10, 0.00, 0.00),
    ROYAL("Маточная рамка", 1103, 16, 0, 0.00, 0.30, 0.00, 0.00),
    PROPOLIS("Прополисная рамка", 1104, 48, 0, 0.00, 0.10, 0.00, 0.50),
    AMETHYST("Аметистовая рамка", 1105, 128, 0, 0.10, 0.40, 0.08, 0.00);

    private final String displayName;
    private final int customModelData;
    private final int maxUses;
    private final int maxHoneyBonus;
    private final double speedBonus;
    private final double qualityWeight;
    private final double qualityBonus;
    private final double stingReduction;

    FrameType(String displayName,
              int customModelData,
              int maxUses,
              int maxHoneyBonus,
              double speedBonus,
              double qualityWeight,
              double qualityBonus,
              double stingReduction) {
        this.displayName = displayName;
        this.customModelData = customModelData;
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

    public int customModelData() {
        return customModelData;
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

    /** Human-readable effect line for items and GUI. */
    public String effectDescription() {
        if (maxHoneyBonus > 0) {
            return "+" + maxHoneyBonus + " к максимуму мёда";
        }
        if (speedBonus > 0) {
            return "+" + Math.round(speedBonus * 100) + "% скорость";
        }
        if (qualityBonus > 0) {
            return "+" + Math.round(qualityBonus * 100) + "% качество";
        }
        if (stingReduction > 0) {
            return "-" + Math.round(stingReduction * 100) + "% укусы";
        }
        return "+" + Math.round(qualityWeight * 100) + "% качество";
    }
}
