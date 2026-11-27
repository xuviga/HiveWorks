package com.xuvigan.hiveworks.storage;

import com.xuvigan.hiveworks.apiary.Apiary;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HivePos;
import com.xuvigan.hiveworks.production.Quality;

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

    /** Updates per-player harvesting statistics. Default keeps older storages compatible. */
    default void recordHarvest(UUID uuid, String playerName, int honey, Quality quality) {
    }

    /** Top harvesters by total honey, then by harvest count. */
    default List<TopEntry> topHarvesters(int limit) {
        return List.of();
    }

    /** Atlas reward thresholds already claimed by the player. */
    default Set<Integer> claimedAtlasRewards(UUID uuid) {
        return Set.of();
    }

    default void claimAtlasReward(UUID uuid, int threshold) {
    }

    record TopEntry(UUID uuid, String name, long honey, long harvests, long legendary) {
    }
}
