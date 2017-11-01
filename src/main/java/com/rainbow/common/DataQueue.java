package com.rainbow.common;

import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuming on 2017/8/29.
 */
public class DataQueue {

    private Jedis client = null;
    private boolean isAuthed = false;

    private final String QUEUE_KEY = "crawler_data";
    private long QUEUE_SIZE;

    public DataQueue(long len) {
        QUEUE_SIZE = len;
    }

    public void selectAndAuth() {
        String nodes = SettingReader.getRedisNodes();
        String password = SettingReader.getRedisPassword();

        String host = nodes.split(";")[0].split(":")[0];
        int port = Integer.parseInt(nodes.split(";")[0].split(":")[1]);

        client = RedisFactory.createSingleNodeJedis(host, port);

        if (!password.equals("None") && !password.equals("")) {
            client.auth(password);
        }
        client.select(12);

        isAuthed = true;
    }

    public void push(String data) throws InterruptedException {
        if (!isAuthed) throw new RuntimeException("db not selected! please invoke selectAndAuth method first!");

        while (true) {
            if (client.llen(QUEUE_KEY) < QUEUE_SIZE) {
                client.lpush(QUEUE_KEY, data);

                break;
            } else {
                Thread.sleep(3 * 1000);
            }
        }

    }

    public String pull() throws InterruptedException {
        if (!isAuthed) throw new RuntimeException("db not selected! please invoke selectAndAuth method first!");

        while (true) {
            if (client.llen(QUEUE_KEY) > 0) {
                return client.lpop(QUEUE_KEY);
            } else {
                Thread.sleep(3 * 1000);
            }
        }

    }

    public long size() {
        if (!isAuthed) throw new RuntimeException("db not selected! please invoke selectAndAuth method first!");

        return client.llen(QUEUE_KEY);
    }

    public void close() {
        client.close();
        isAuthed = false;
    }

    public static void main(String[] args) throws InterruptedException {
        DataQueue queue = new DataQueue(100000);
        queue.selectAndAuth();

        List<String> list = new ArrayList<>();
        list.add("1111<,>biz<=>MzA4MDM4MTkwMw==<;>mid<=>208191769<;>idx<=>3<;>sn<=>43852baebd7be42eac697d965cffa887");
        list.add("1111<,>biz<=>MzI1NjEwMjk0NQ==<;>mid<=>2654449925<;>idx<=>5<;>sn<=>7c0993241ebc7c192b88f405885e531f");
        list.add("1111<,>biz<=>MzIxODM5NDExNw==<;>mid<=>2247492381<;>idx<=>1<;>sn<=>5ac710981cf8bde1c5b3a4e546c8bfc2");
        list.add("1111<,>biz<=>MzIxODM5NDExNw==<;>mid<=>2247492381<;>idx<=>1<;>sn<=>5ac710981cf8bde1c5b3a4e546c8bfc2");
        list.add("1111<,>biz<=>MjM5NzQzNzExMw==<;>mid<=>2653654880<;>idx<=>1<;>sn<=>ea9cc32e30809bcb6fb83345b667a0c1");
        list.add("1111<,>biz<=>MzAxOTgyMjQ5NQ==<;>mid<=>2649823675<;>idx<=>2<;>sn<=>e8caed1b819bf28014724a525abc8304");
        list.add("1111<,>biz<=>MzIzMDc0NzA5OA==<;>mid<=>2247484020<;>idx<=>2<;>sn<=>8734e918643d2688d21c0f709ddfa7cc");
        list.add("1111<,>biz<=>MzIzMDc0NzA5OA==<;>mid<=>2247484020<;>idx<=>2<;>sn<=>8734e918643d2688d21c0f709ddfa7cc");

        for (String s : list) {
            queue.push(s);
            Thread.sleep(2 * 1000);
        }
    }


}
