package com.xuvigan.hiveworks.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reads the catching net.
 */
public final class NetItem {

    public boolean isNet(ItemStack item) {
        if (item == null || item.getType() != Material.BRUSH || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return "net".equals(meta.getPersistentDataContainer().get(PdcKeys.KIND, PersistentDataType.STRING));
    }
}
