package com.rainbow.config;

import com.alibaba.fastjson.JSON;
import com.google.common.io.Files;
import org.apache.commons.lang.StringUtils;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuming on 2017/9/25.
 */
public class SetupConfig {

    private static final Logger logger = LoggerFactory.getLogger(SetupConfig.class);

    public String spiderDir = null;
    public String dataDir = null;
    public boolean resume = false;
    public int workers = 2;
    public boolean stopOnEmpty = true;
    public String proxyHost = null;
    public int proxyPort = -1;
    public boolean includeBinary = false;
    public Request request = new Request();
    public List<StorageConfig> storage = new ArrayList<>();


    public static class Request {
        public int delayTime = 200;
        private List<Header> headers = new ArrayList<>();

        public static class Header {
            public String key = null;
            public String value = null;
        }

        public List<org.apache.http.Header> getHeaders() {
            List<org.apache.http.Header> res = new ArrayList<>();

            for (Header header : headers) {
                res.add(new BasicHeader(header.key, header.value));
            }

            return res;
        }
    }

    public static class StorageConfig {
        public String type = "";
        public String value = "";
    }

    public static SetupConfig load(File file) throws IOException {
        List<String> list = Files.readLines(file, Charset.forName("utf-8"));
        String text = StringUtils.join(list, "");

        return load(text);
    }

    public static SetupConfig load(String text) throws IOException {
        return JSON.parseObject(text, SetupConfig.class);
    }

    public static SetupConfig create4Test() {
        SetupConfig setupConfig = new SetupConfig();

        String tmpDir = System.getProperty("java.io.tmpdir");
        String dataDir = tmpDir + "/pxene-crawler";

        logger.info("dataDir: " + dataDir);
        setupConfig.dataDir = dataDir;

        StorageConfig storageConfig = new StorageConfig();
        storageConfig.type = "console";
        storageConfig.value = "console";

        setupConfig.storage.add(storageConfig);

        return setupConfig;
    }

    public boolean check() {
        if (StringUtils.isBlank(dataDir)) {
            logger.error("data dir cannot be empty!");
            return false;
        }

        if (StringUtils.isBlank(spiderDir)) {
            logger.error("spider dir cannot be empty!");
            return false;
        }

        return true;
    }

}


