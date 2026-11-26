package com.xuvigan.hiveworks.item;

import com.xuvigan.hiveworks.hive.FrameType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Reads frame items created by ItemFactory.
 */
public final class FrameItem {

    public Optional<FrameItemData> read(ItemStack item) {
        if (item == null || item.getType() != Material.HONEYCOMB || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!"frame".equals(pdc.get(PdcKeys.KIND, PersistentDataType.STRING))) {
            return Optional.empty();
        }
        String typeName = pdc.get(PdcKeys.FRAME, PersistentDataType.STRING);
        Integer uses = pdc.get(PdcKeys.USES, PersistentDataType.INTEGER);
        if (typeName == null || uses == null) {
            return Optional.empty();
        }
        try {
            FrameType type = FrameType.valueOf(typeName);
            int remaining = Math.max(1, Math.min(type.maxUses(), uses));
            return Optional.of(new FrameItemData(type, remaining));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public record FrameItemData(FrameType type, int usesLeft) {
    }
}
