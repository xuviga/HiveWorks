package com.xuvigan.hiveworks.storage;

import com.xuvigan.hiveworks.apiary.Apiary;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HivePos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Persistence seam: SQLite (default) or YAML.
 */
public interface Storage {

    void init() throws Exception;

    List<HiveData> loadAll();

    Set<String> discoveredSpecies(UUID uuid);

    void discoverSpecies(UUID uuid, String playerName, String speciesId);

    List<Apiary> loadApiaries();

    void saveApiary(Apiary apiary);

    void deleteApiary(UUID id);

    void save(HiveData hive);

    void delete(HivePos pos);

    void close();
}
