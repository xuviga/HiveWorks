package com.xuvigan.hiveworks.storage;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.config.ConfigManager;

public final class StorageFactory {

    private StorageFactory() {
    }

    public static Storage create(HiveWorksPlugin plugin, ConfigManager config) throws Exception {
        Storage storage = switch (config.storageType()) {
            case SQLITE -> new SqliteStorage(plugin, config);
            case YAML -> new YamlStorage(plugin);
        };
        storage.init();
        return storage;
    }
}
