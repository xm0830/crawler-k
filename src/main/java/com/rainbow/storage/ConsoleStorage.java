package com.rainbow.storage;

import com.rainbow.common.DataFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by xuming on 2017/10/12.
 */
public class ConsoleStorage extends BaseStorage {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleStorage.class);

    public ConsoleStorage(String value) {
        super(value);
    }

    @Override
    public void init() throws IOException {

    }

    @Override
    public void save(DataFrame df, String rowKey) throws IOException {
        logger.info("rowKey: {}\nfetch: {}", rowKey, df.toString());

        if (rowKey.endsWith("_")) {
            logger.error("rowKey: {} cannot be empty!", rowKey);
        }

    }

    @Override
    public void destroy() throws IOException {

    }
}
