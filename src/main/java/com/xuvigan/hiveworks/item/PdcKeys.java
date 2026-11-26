package com.xuvigan.hiveworks.item;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Central registry of PersistentDataContainer keys.
 */
public final class PdcKeys {

    public static NamespacedKey KIND;
    public static NamespacedKey BEE_UUID;
    public static NamespacedKey SPECIES;
    public static NamespacedKey GENES;
    public static NamespacedKey PURITY;
    public static NamespacedKey QUALITY;
    public static NamespacedKey FRAME;
    public static NamespacedKey USES;

    private PdcKeys() {
    }

    public static void init(Plugin plugin) {
        if (KIND != null) {
            return;
        }
        KIND = new NamespacedKey(plugin, "kind");
        BEE_UUID = new NamespacedKey(plugin, "bee_uuid");
        SPECIES = new NamespacedKey(plugin, "species");
        GENES = new NamespacedKey(plugin, "genes");
        PURITY = new NamespacedKey(plugin, "purity");
        QUALITY = new NamespacedKey(plugin, "quality");
        FRAME = new NamespacedKey(plugin, "frame");
        USES = new NamespacedKey(plugin, "uses");
    }
}
