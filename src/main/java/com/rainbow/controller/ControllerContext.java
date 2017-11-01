package com.rainbow.controller;

import com.rainbow.config.SetupConfig;
import com.rainbow.config.SpiderConfig;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by xuming on 2017/9/29.
 */
public class ControllerContext {

    private Map<String, SpiderConfig> spiderConfigMap = null;
    private DB db = null;
    private ConcurrentMap<String, String> varMap = null;
    private List<SetupConfig.StorageConfig> storageConfigs = null;


    public ControllerContext(Map<String, SpiderConfig> map, SetupConfig config) {
        this.spiderConfigMap = map;

        this.db = DBMaker.newFileDB(new File(config.dataDir + "/dbfile")).make();
        this.varMap = db.getHashMap("hash");
        this.storageConfigs = config.storage;
    }

    public Map<String, SpiderConfig> getSpiderConfigMap() {
        return spiderConfigMap;
    }

    public ConcurrentMap<String, String> getVarMap() {
        return varMap;
    }

    public List<SetupConfig.StorageConfig> getStorageConfigs() {
        return storageConfigs;
    }

    public void reset() {
        if (db != null) {
            varMap.clear();
        }
    }
}
