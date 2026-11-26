package com.xuvigan.hiveworks.bee;

import java.util.UUID;

/**
 * Data carried by a "bee in a jar" item.
 */
public record BeeItemData(UUID id, String speciesId, Genes genes, int purity) {
}
