package com.rainbow.config;

import com.alibaba.fastjson.JSON;
import com.google.common.io.Files;
import org.apache.commons.lang.StringUtils;
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
public class SpiderConfig {
    private static final Logger logger = LoggerFactory.getLogger(SpiderConfig.class);

    public String id = null;
    public String desc = null;
    public String seedUrl = null;
    public boolean enableProxy = false;
    public List<Page> pages = new ArrayList<>();

    public static class Page {
        public String primaryKey = null;
        public String outputUrlFilter = null;
        public String customOutputUrls = null;
        public List<Prop> props = new ArrayList<>();
        public List<Field> fields = new ArrayList<>();

        public boolean isLastPage = false;

        public static class Prop {
            public String name = null;
            public String value = null;
        }

        public static class Field {
            public String name = null;
            public String desc = null;
            public String selector = null;
            public boolean isVar = false;
            public String transfer = null;
            public boolean repeated = false;
        }

    }

    public static SpiderConfig load(File file) throws IOException {
        List<String> list = Files.readLines(file, Charset.forName("utf-8"));
        String text = StringUtils.join(list, "");

        return load(text);
    }

    public static SpiderConfig load(String text) {
        return JSON.parseObject(text, SpiderConfig.class);
    }

    public boolean check() {
        if (StringUtils.isBlank(id)) {
            logger.error("spider id cannot be empty!");
            return false;
        }
        if (StringUtils.isBlank(seedUrl)) {
            logger.error("spdier seedUrl cannot be empty!");
            return false;
        }
        if (pages.isEmpty()) {
            logger.error("pages cannot be empty!");
            return false;
        }

//        for (int i = 0; i < pages.size(); i++) {
//            if (i != pages.size() - 1) {
//                if (StringUtils.isBlank(pages.get(i).outputUrlFilter)) {
//                    logger.error("only the last page's outputUrlFilter can be empty!");
//                    return false;
//                }
//            }
//        }

        List<Page.Field> fields = pages.get(pages.size() - 1).fields;
        if (fields.isEmpty()) {
            logger.error("the last page's fields cannot be empty!");
            return false;
        }
        for (Page.Field field : fields) {
            if (StringUtils.isBlank(field.name)) {
                logger.error("field name cannot be empty!");
                return false;
            }
            if (StringUtils.isBlank(field.selector) && StringUtils.isBlank(field.transfer)) {
                logger.error("field selector and transfer cannot be both empty!");
                return false;
            }

            if (field.isVar && field.repeated) {
                logger.error("repeated field cannot be Var field!");
                return false;
            }
        }

        return true;
    }

}
