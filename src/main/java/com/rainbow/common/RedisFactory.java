package com.rainbow.common;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.Set;

/**
 * Created by xuming on 2017/2/14.
 */
class RedisFactory {

    public static Jedis createSingleNodeJedis(String host) {
        return createSingleNodeJedis(host, 6379);
    }

    public static Jedis createSingleNodeJedis(String host, int port) {
        return createSingleNodeJedis(host, port, 60000);
    }

    public static Jedis createSingleNodeJedis(String host, int port, int timeout) {
        return new Jedis(host, port, timeout);
    }

    public static JedisCluster createClusterJedis(Set<HostAndPort> nodeInfos) {
        return createClusterJedis(nodeInfos, 60000);
    }

    public static JedisCluster createClusterJedis(Set<HostAndPort> nodeInfos, int timeout) {
        return new JedisCluster(nodeInfos, timeout);
    }

}
