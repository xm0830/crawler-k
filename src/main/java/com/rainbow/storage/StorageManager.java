package com.rainbow.storage;

import com.rainbow.common.DataFrame;
import com.rainbow.config.SetupConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xuming on 2017/10/12.
 */
public class StorageManager {
    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);

    private List<BaseStorage> storages = new ArrayList<>();

    private static Map<String, Class<? extends BaseStorage>> globalClass = new HashMap<>();

    private StorageManager() { }

    public static StorageManager create(List<SetupConfig.StorageConfig> configs) throws IOException {
        StorageManager manager = new StorageManager();
        manager.init(configs);
        return manager;
    }

    public static void registExtStorage(String type, Class<? extends BaseStorage> storageClass) {
        globalClass.put(type, storageClass);
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

                default:
                    if (globalClass.containsKey(config.type)) {
                        Class<? extends BaseStorage> clazz = globalClass.get(config.type);
                        try {
                            Constructor<? extends BaseStorage> constructor = clazz.getDeclaredConstructor(String.class);
                            BaseStorage baseStorage = constructor.newInstance(config.value);
                            baseStorage.init();

                            storages.add(baseStorage);
                        } catch (NoSuchMethodException e) {
                            logger.error("constructor of {} only support one String parameter!", clazz.getName(), e);
                        } catch (Exception e) {
                            logger.error("new instance for {} error!", clazz.getName());
                        }
                    }

                    break;
            }
        }
    }
}