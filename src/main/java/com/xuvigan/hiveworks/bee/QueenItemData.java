package com.xuvigan.hiveworks.bee;

import java.util.UUID;

/**
 * Data carried by a "Queen" item. It can be installed into a hive queen slot.
 */
public record QueenItemData(UUID id, String speciesId, Genes genes, int purity) {

    public QueenItemData {
        purity = Math.max(0, Math.min(100, purity));
    }

    public BeeItemData asBee() {
        return new BeeItemData(id, speciesId, genes, purity);
    }
}
