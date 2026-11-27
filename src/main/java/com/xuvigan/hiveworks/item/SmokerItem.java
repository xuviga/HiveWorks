package com.xuvigan.hiveworks.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reads the smoker tool. Holding one in the inventory halves the sting chance.
 */
public final class SmokerItem {

    public boolean isSmoker(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return "smoker".equals(meta.getPersistentDataContainer().get(PdcKeys.KIND, PersistentDataType.STRING));
    }
}
