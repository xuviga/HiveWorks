package com.xuvigan.hiveworks.product;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Three mead variants. Buffs are disabled by features.mead-buffs.
 */
public enum MeadType {

    MEADOW("meadow", 2111, "SPEED", 20 * 180, 0),
    FOREST("forest", 2112, "REGENERATION", 20 * 10, 0),
    ELITE("elite", 2113, "NIGHT_VISION", 20 * 300, 0);

    private final String id;
    private final int customModelData;
    private final String effectName;
    private final int durationTicks;
    private final int amplifier;

    MeadType(String id, int customModelData, String effectName, int durationTicks, int amplifier) {
        this.id = id;
        this.customModelData = customModelData;
        this.effectName = effectName;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    public String id() {
        return id;
    }

    public int customModelData() {
        return customModelData;
    }

    public String effectName() {
        return effectName;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public int amplifier() {
        return amplifier;
    }

    public void apply(Player player) {
        PotionEffectType effect = PotionEffectType.getByName(effectName);
        if (effect != null) {
            player.addPotionEffect(new PotionEffect(effect, durationTicks, amplifier, false, true, true));
        }
    }

    public static MeadType parse(String raw) {
        if (raw == null) {
            return null;
        }
        for (MeadType type : values()) {
            if (type.name().equalsIgnoreCase(raw) || type.id.equalsIgnoreCase(raw)) {
                return type;
            }
        }
        return null;
    }
}
