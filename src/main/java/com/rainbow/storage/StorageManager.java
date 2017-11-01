package com.rainbow.storage;

import com.rainbow.common.DataFrame;
import com.rainbow.config.SetupConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuming on 2017/10/12.
 */
public class StorageManager {
    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);

    private List<BaseStorage> storages = new ArrayList<>();

    private StorageManager() { }

    public static StorageManager create(List<SetupConfig.StorageConfig> configs) throws IOException {
        StorageManager manager = new StorageManager();
        manager.init(configs);
        return manager;
    }

    public void save(DataFrame df,  String rowKey) throws IOException {
        for (BaseStorage storage : storages) {
            storage.save(df, rowKey);
        }
    }

    public void close() throws IOException {
        for (BaseStorage storage : storages) {
            storage.destroy();
        }
    }

    private void init(List<SetupConfig.StorageConfig> configs) throws IOException {
        for (SetupConfig.StorageConfig config : configs) {
            logger.debug("storage type: {}, storage value: {}", config.type, config.value);
            switch (config.type) {
                case "hbase":
                    HbaseStorage hbaseStorage = new HbaseStorage(config.value);
                    hbaseStorage.init();
                    storages.add(hbaseStorage);

                    break;

                case "console":
                    ConsoleStorage consoleStorage = new ConsoleStorage(config.value);
                    consoleStorage.init();
                    storages.add(consoleStorage);

                    break;
            }
        }
    }
}
