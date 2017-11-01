package com.rainbow.storage;

import com.rainbow.common.DataFrame;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by xuming on 2017/10/12.
 */
public class HbaseStorage extends BaseStorage {

    private static final Logger logger = LoggerFactory.getLogger(HbaseStorage.class);

    private Connection connection = null;
    private Table table = null;

    private byte[] familyBytes = Bytes.toBytes("info");

    public HbaseStorage(String value) {
        super(value);
    }

    @Override
    public void init() throws IOException {
        String[] tokens = value.split(";", -1);
        String tableName = tokens[0];
        String zkHosts = tokens[1];
        String zkPort = tokens[2];

        Configuration conf = HBaseConfiguration.create();
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        conf.addResource(new Path(System.getenv("HADOOP_HOME") + "/etc/hadoop/core-site.xml"));
        conf.addResource(new Path(System.getenv("HADOOP_HOME") + "/etc/hadoop/hdfs-site.xml"));
        conf.set("hbase.zookeeper.quorum", zkHosts);
        conf.set("hbase.zookeeper.property.clientPort", zkPort);
        conf.setInt("hbase.hconnection.threads.core", 0);

        connection = ConnectionFactory.createConnection(conf);
        table = connection.getTable(TableName.valueOf(tableName));
    }

    @Override
    public void save(DataFrame df, String rowKey) throws IOException {
        if (!rowKey.endsWith("_")) {
            byte[] rowKeyBytes = Bytes.toBytes(StringUtils.reverse(rowKey));
            if (!table.exists(new Get(rowKeyBytes))) {
                if (df.rows() > 0) {
                    Put put = new Put(rowKeyBytes);
                    for (int i = 0; i < df.rows(); i++) {
                        List<String> row = df.getRow(i);
                        for (int j = 0; j < row.size(); j++) {
                            String value = row.get(j);
                            String name = df.getColumnNames().get(j);

                            put.addColumn(familyBytes, Bytes.toBytes(name), Bytes.toBytes(value));
                        }
                    }

                    table.put(put);
                    logger.info("add data to hbase successfully, row key: {}", rowKey);
                } else {
                    logger.warn("fetched data is empty!");
                }
            } else {
                logger.info("row key: {} already exists, skipped to add", rowKey);
            }

        } else {
            logger.warn("rowKey: {} is empty, skipped to save", rowKey);
        }
    }

    @Override
    public void destroy() throws IOException {
        if (table != null) {
            table.close();
        }

        if (connection != null) {
            connection.close();
        }
    }
}
