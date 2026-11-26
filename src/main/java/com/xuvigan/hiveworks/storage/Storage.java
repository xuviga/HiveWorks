package com.xuvigan.hiveworks.storage;

import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HivePos;

import java.util.List;

/**
 * Persistence seam: SQLite (default) or YAML.
 */
public interface Storage {

    void init() throws Exception;

    List<HiveData> loadAll();

    void save(HiveData hive);

    void delete(HivePos pos);

    void close();
}
