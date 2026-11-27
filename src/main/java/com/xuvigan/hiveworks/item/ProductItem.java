package com.xuvigan.hiveworks.item;

import com.xuvigan.hiveworks.product.ProductType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reads product items created by ItemFactory.
 */
public final class ProductItem {

    public boolean is(ItemStack item, ProductType type) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!"product".equals(meta.getPersistentDataContainer().get(PdcKeys.KIND, PersistentDataType.STRING))) {
            return false;
        }
        String product = meta.getPersistentDataContainer().get(PdcKeys.PRODUCT, PersistentDataType.STRING);
        return type.name().equals(product);
    }

    public boolean isRoyalJelly(ItemStack item) {
        return is(item, ProductType.ROYAL_JELLY);
    }

    public boolean isPollen(ItemStack item) {
        return is(item, ProductType.POLLEN);
    }

    public boolean isRoyalCell(ItemStack item) {
        return is(item, ProductType.ROYAL_CELL);
    }
}
