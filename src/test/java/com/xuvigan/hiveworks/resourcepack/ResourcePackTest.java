package com.xuvigan.hiveworks.resourcepack;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackTest {

    @Test
    void packIsPackagedWithManifestAndModels() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/pack/hiveworks-pack.zip")) {
            assertNotNull(input, "pack/hiveworks-pack.zip must be on the classpath");
            Set<String> entries = new HashSet<>();
            try (ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    entries.add(entry.getName());
                }
            }
            assertTrue(entries.contains("pack.mcmeta"), "pack.mcmeta at zip root");
            assertTrue(entries.contains("assets/hiveworks/items/bee_jar_forest.json"), "modern item definition");
            assertTrue(entries.contains("assets/minecraft/models/item/honeycomb.json"), "legacy override");
            long models = entries.stream()
                    .filter(name -> name.startsWith("assets/hiveworks/models/item/") && name.endsWith(".json"))
                    .count();
            assertTrue(models >= 33, "expected at least 33 item models, got " + models);
        }
    }
}
