package com.rainbow.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;

/**
 * Created by xuming on 2017/11/7.
 */
public class DBManager {

    private static final Logger logger = LoggerFactory.getLogger(DBManager.class);

    private Connection connection = null;

    public void init(String driver, String url, String name, String password) throws SQLException {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            logger.error("load mysql Driver error!", e);
        }
        connection = DriverManager.getConnection(url, name, password);
    }

    public boolean exists(String tableName, String condition) throws SQLException {
        String sql = "select count(*) from " + tableName + " where " + condition;
        PreparedStatement preparedStatement = connection.prepareStatement(sql);

//        logger.info("sql: {}", sql);

        ResultSet rs = preparedStatement.executeQuery();
        rs.next();

        int count = rs.getInt(1);

        rs.close();
        preparedStatement.close();

        return count > 0;
    }

    public void insert(String tableName, Map<String, String> map) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("insert into ")
                .append(tableName)
                .append(" (");

        String names = "";
        String values = "";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            if (names.isEmpty()) {
                names += name;
                values +=  value;
            } else {
                names += ("," + name);
                values += ("','" + value);
            }
        }

        sqlBuilder.append(names).append(") values('").append(values).append("')");

//        logger.info("sql: {}", sqlBuilder.toString());

        PreparedStatement preparedStatement = connection.prepareStatement(sqlBuilder.toString());
        preparedStatement.execute();
        preparedStatement.close();
    }

    public void destroy() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

}
