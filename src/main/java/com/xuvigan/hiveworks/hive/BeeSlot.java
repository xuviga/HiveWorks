package com.xuvigan.hiveworks.hive;

import com.xuvigan.hiveworks.bee.Genes;

import java.util.UUID;

/**
 * A bee sitting in a hive slot.
 */
public record BeeSlot(UUID beeId, String speciesId, Genes genes, int purity) {
}
