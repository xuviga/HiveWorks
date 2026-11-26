package com.xuvigan.hiveworks.bee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenesTest {

    @Test
    void genesAreClamped() {
        assertEquals(new Genes(100, 0, 50, 50), new Genes(150, -20, 50, 50));
    }

    @Test
    void serializationRoundTrip() {
        Genes genes = new Genes(12, 34, 56, 78);
        assertEquals(genes, Genes.parse(genes.serialize()));
    }

    @Test
    void malformedInputFallsBackToDefaults() {
        assertEquals(Genes.DEFAULT, Genes.parse("oops"));
        assertEquals(Genes.DEFAULT, Genes.parse(null));
    }
}
