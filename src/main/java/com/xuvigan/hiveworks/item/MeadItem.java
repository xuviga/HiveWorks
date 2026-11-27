package com.xuvigan.hiveworks.item;

import com.xuvigan.hiveworks.product.MeadType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Reads mead bottles created by ItemFactory.
 */
public final class MeadItem {

    public Optional<MeadType> read(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (!"mead".equals(meta.getPersistentDataContainer().get(PdcKeys.KIND, PersistentDataType.STRING))) {
            return Optional.empty();
        }
        String type = meta.getPersistentDataContainer().get(PdcKeys.PRODUCT, PersistentDataType.STRING);
        return Optional.ofNullable(MeadType.parse(type));
    }
}
