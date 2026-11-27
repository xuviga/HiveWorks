package com.xuvigan.hiveworks.storage;

import com.xuvigan.hiveworks.apiary.Apiary;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HivePos;
import com.xuvigan.hiveworks.production.Quality;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Serialises every storage call.
 *
 * <p>SQLite (and the YAML file) is backed by a single connection/state object.
 * On Folia hives are read and written from several region threads, so a shared
 * monitor is required around the whole persistence seam.</p>
 */
final class SynchronizedStorage implements Storage {

    private final Storage delegate;
    private final Object lock = new Object();

    SynchronizedStorage(Storage delegate) {
        this.delegate = delegate;
    }

    @Override
    public void init() throws Exception {
        synchronized (lock) {
            delegate.init();
        }
    }

    @Override
    public List<HiveData> loadAll() {
        synchronized (lock) {
            return delegate.loadAll();
        }
    }

    @Override
    public Set<String> discoveredSpecies(UUID uuid) {
        synchronized (lock) {
            return delegate.discoveredSpecies(uuid);
        }
    }

    @Override
    public void discoverSpecies(UUID uuid, String playerName, String speciesId) {
        synchronized (lock) {
            delegate.discoverSpecies(uuid, playerName, speciesId);
        }
    }

    @Override
    public List<Apiary> loadApiaries() {
        synchronized (lock) {
            return delegate.loadApiaries();
        }
    }

    @Override
    public void saveApiary(Apiary apiary) {
        synchronized (lock) {
            delegate.saveApiary(apiary);
        }
    }

    @Override
    public void deleteApiary(UUID id) {
        synchronized (lock) {
            delegate.deleteApiary(id);
        }
    }

    @Override
    public void save(HiveData hive) {
        synchronized (lock) {
            delegate.save(hive);
        }
    }

    @Override
    public void delete(HivePos pos) {
        synchronized (lock) {
            delegate.delete(pos);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            delegate.close();
        }
    }

    @Override
    public void recordHarvest(UUID uuid, String playerName, int honey, Quality quality) {
        synchronized (lock) {
            delegate.recordHarvest(uuid, playerName, honey, quality);
        }
    }

    @Override
    public List<TopEntry> topHarvesters(int limit) {
        synchronized (lock) {
            return delegate.topHarvesters(limit);
        }
    }

    @Override
    public Set<Integer> claimedAtlasRewards(UUID uuid) {
        synchronized (lock) {
            return delegate.claimedAtlasRewards(uuid);
        }
    }

    @Override
    public void claimAtlasReward(UUID uuid, int threshold) {
        synchronized (lock) {
            delegate.claimAtlasReward(uuid, threshold);
        }
    }
}
