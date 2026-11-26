package com.xuvigan.hiveworks.bee;

import com.xuvigan.hiveworks.item.PdcKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads bee items. Creation lives in ItemFactory.
 */
public final class BeeItem {

    public Optional<BeeItemData> read(ItemStack item) {
        if (item == null || item.getType() != Material.GLASS_BOTTLE || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!"bee".equals(pdc.get(PdcKeys.KIND, PersistentDataType.STRING))) {
            return Optional.empty();
        }

        String uuidRaw = pdc.get(PdcKeys.BEE_UUID, PersistentDataType.STRING);
        String speciesId = pdc.get(PdcKeys.SPECIES, PersistentDataType.STRING);
        String genesRaw = pdc.get(PdcKeys.GENES, PersistentDataType.STRING);
        Integer purity = pdc.get(PdcKeys.PURITY, PersistentDataType.INTEGER);
        if (uuidRaw == null || speciesId == null || genesRaw == null || purity == null) {
            return Optional.empty();
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidRaw);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }

        return Optional.of(new BeeItemData(
                uuid,
                speciesId,
                Genes.parse(genesRaw),
                Math.max(0, Math.min(100, purity))
        ));
    }
}
