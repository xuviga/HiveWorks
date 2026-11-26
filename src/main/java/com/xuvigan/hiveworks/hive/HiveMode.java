package com.xuvigan.hiveworks.hive;

import org.bukkit.Material;

/**
 * Hive working modes. GUARD arrives in 0.2.
 */
public enum HiveMode {

    APIARY("mode-apiary", Material.HONEY_BOTTLE),
    POLLINATION("mode-pollination", Material.SUNFLOWER),
    COLLECTION("mode-collection", Material.DANDELION);

    private final String messageKey;
    private final Material icon;

    HiveMode(String messageKey, Material icon) {
        this.messageKey = messageKey;
        this.icon = icon;
    }

    public String messageKey() {
        return messageKey;
    }

    public Material icon() {
        return icon;
    }

    public HiveMode next() {
        HiveMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
