package com.rainbow.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by xuming on 2017/8/9.
 */
public class SettingReader {

    private static Map<String, String> map = new HashMap<>();

    static {
        try {
            load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void load() throws IOException {
        String home = new File("").getCanonicalPath();

        Properties properties = new Properties();
        properties.load(new FileInputStream(home + "/conf/setting.properties"));
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String key = propertyNames.nextElement().toString();
            map.put(key, properties.getProperty(key));
        }
    }

    private static String get(String key) {
        return map.get(key);
    }

    public static String getRedisNodes() {
        return get("redis_nodes");
    }

    public static String getRedisPassword() {
        return get("redis_password");
    }
}
