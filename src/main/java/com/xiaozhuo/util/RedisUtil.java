package com.xiaozhuo.util;

import com.xiaozhuo.dao.CommentDao;
import com.xiaozhuo.dao.impl.CommentDaoImpl;
import com.xiaozhuo.entity.VideoComment;
import com.xiaozhuo.exception.DatabaseException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class RedisUtil {
    private static JedisPool jedisPool;

    static {
        try {
            Properties prop = new Properties();
            InputStream is = RedisUtil.class.getClassLoader().getResourceAsStream("db.properties");
            if (is != null) {
                prop.load(is);

                String host = prop.getProperty("redis.host", "localhost");
                int port = Integer.parseInt(prop.getProperty("redis.port", "6379"));
                String password = prop.getProperty("redis.password", "");
                int database = Integer.parseInt(prop.getProperty("redis.database", "0"));
                int maxTotal = Integer.parseInt(prop.getProperty("redis.maxTotal", "50"));
                int maxIdle = Integer.parseInt(prop.getProperty("redis.maxIdle", "10"));

                JedisPoolConfig poolConfig = new JedisPoolConfig();
                poolConfig.setMaxTotal(maxTotal);
                poolConfig.setMaxIdle(maxIdle);

                if (password != null && !password.isEmpty()) {
                    jedisPool = new JedisPool(poolConfig, host, port, 2000, password, database);
                } else {
                    jedisPool = new JedisPool(poolConfig, host, port, 2000, null, database);
                }

                System.out.println("Redis连接池初始化成功！");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError("Redis连接池初始化失败：" + e.getMessage());
        }
    }

    public static Jedis getJedis() {
        return jedisPool.getResource();
    }

    public static void close(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }

    public static String get(String key) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(jedis);
        }
    }

    public static String set(String key, String value) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.set(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(jedis);
        }
    }

    public static String setex(String key, int seconds, String value) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.setex(key, seconds, value);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(jedis);
        }
    }

    public static Boolean exists(String key) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.exists(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            close(jedis);
        }
    }

    public static Long del(String... keys) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.del(keys);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        } finally {
            close(jedis);
        }
    }

    public static Long expire(String key, int seconds) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.expire(key, seconds);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        } finally {
            close(jedis);
        }
    }

    /**
     * 根据模式查询键列表
     * @param pattern 匹配模式，如 "comment:video:*"
     * @return 匹配的键集合
     */
    public static java.util.Set<String> keys(String pattern) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.keys(pattern);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptySet();
        } finally {
            close(jedis);
        }
    }
}

