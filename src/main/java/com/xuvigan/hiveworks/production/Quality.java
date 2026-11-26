package com.xuvigan.hiveworks.production;

/**
 * Honey quality tiers.
 */
public enum Quality {

    ORDINARY("<gray>Обычный</gray>", 2000),
    GOOD("<green>Хороший</green>", 2001),
    ELITE("<aqua>Элитный</aqua>", 2002),
    LEGENDARY("<gold>Легендарный</gold>", 2003);

    private final String displayName;
    private final int customModelData;

    Quality(String displayName, int customModelData) {
        this.displayName = displayName;
        this.customModelData = customModelData;
    }

    public String displayName() {
        return displayName;
    }

    public int customModelData() {
        return customModelData;
    }
}
