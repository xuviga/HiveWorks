package com.xuvigan.hiveworks.item;

import com.xuvigan.hiveworks.bee.Genes;
import com.xuvigan.hiveworks.bee.QueenItemData;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads "Queen" items created by ItemFactory.
 */
public final class QueenItem {

    public Optional<QueenItemData> read(ItemStack item) {
        if (item == null || item.getType() != Material.GLASS_BOTTLE || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!"queen".equals(pdc.get(PdcKeys.KIND, PersistentDataType.STRING))) {
            return Optional.empty();
        }
        String uuidRaw = pdc.get(PdcKeys.BEE_UUID, PersistentDataType.STRING);
        String speciesId = pdc.get(PdcKeys.SPECIES, PersistentDataType.STRING);
        String genesRaw = pdc.get(PdcKeys.GENES, PersistentDataType.STRING);
        Integer purity = pdc.get(PdcKeys.PURITY, PersistentDataType.INTEGER);
        if (uuidRaw == null || speciesId == null || genesRaw == null || purity == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new QueenItemData(
                    UUID.fromString(uuidRaw),
                    speciesId,
                    Genes.parse(genesRaw),
                    Math.max(0, Math.min(100, purity))
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
