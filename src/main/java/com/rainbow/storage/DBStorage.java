package com.rainbow.storage;

import com.rainbow.common.DBManager;
import com.rainbow.common.DataFrame;
import com.rainbow.storage.BaseStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xuming on 2017/11/7.
 */
public class DBStorage extends BaseStorage {

    private static final Logger logger = LoggerFactory.getLogger(DBStorage.class);

    private String driver = null;
    private String url = null;
    private String user = null;
    private String password = null;
    private String tableName = null;

    private DBManager manager = null;

    public DBStorage(String value) {
        super(value);
    }

    @Override
    public void init() throws IOException {
        String[] tokens = value.split(";", -1);
        if (tokens.length == 5) {
            driver = tokens[0];
            url = tokens[1];
            user = tokens[2];
            password = tokens[3];
            tableName = tokens[4];
        }

        manager = new DBManager();
        try {
            manager.init(driver, url, user, password);
        } catch (SQLException e) {
            logger.error("init db manager error!", e);
        }
    }

    @Override
    public void save(DataFrame dataFrame, String s) throws IOException {
        try {
            if (!s.endsWith("_")) {
                if (!manager.exists(tableName, "article_id='" + s + "'")) {
                    Map<String, String> map = new HashMap<>();
                    map.put("article_id", s);

                    for (int i = 0; i < dataFrame.rows(); i++) {
                        List<String> row = dataFrame.getRow(i);
                        for (int i1 = 0; i1 < row.size(); i1++) {
                            String name = dataFrame.getColumnNames().get(i1);
                            String value = row.get(i1).replaceAll("'", "''");

                            map.put(name, value);
                        }

                        manager.insert(tableName, map);
                        map.clear();
                    }
                } else {
                    logger.info("article for rowkey: {} already exists, skipped!", s);
                }

            } else {
                logger.warn("rowkey is null: {}", s);
            }
        } catch (SQLException e) {
            logger.error("exception happened!", e);
        }

    }

    @Override
    public void destroy() throws IOException {
        try {
            manager.destroy();
        } catch (SQLException e) {
            logger.error("exception happened!", e);
        }
    }
}
