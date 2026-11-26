package com.xuvigan.hiveworks.hive;

/**
 * Hive tiers: capacity, frame slots and bonuses.
 */
public enum HiveTier {

    I(3, 1, 0.00, 0.00, 0.00, "Дубовый"),
    II(4, 2, 0.10, 0.00, 0.00, "Еловый"),
    III(5, 3, 0.10, 0.10, 0.00, "Тёмный дуб"),
    IV(6, 4, 0.10, 0.10, 0.15, "Аметистовый");

    private final int beeSlots;
    private final int frameSlots;
    private final double speedBonus;
    private final double qualityBonus;
    private final double rareChance;
    private final String materialName;

    HiveTier(int beeSlots, int frameSlots, double speedBonus, double qualityBonus, double rareChance, String materialName) {
        this.beeSlots = beeSlots;
        this.frameSlots = frameSlots;
        this.speedBonus = speedBonus;
        this.qualityBonus = qualityBonus;
        this.rareChance = rareChance;
        this.materialName = materialName;
    }

    public int beeSlots() {
        return beeSlots;
    }

    public int frameSlots() {
        return frameSlots;
    }

    public double speedBonus() {
        return speedBonus;
    }

    public double qualityBonus() {
        return qualityBonus;
    }

    public double rareChance() {
        return rareChance;
    }

    public String display() {
        return materialName + " улей (" + name() + ")";
    }
}
