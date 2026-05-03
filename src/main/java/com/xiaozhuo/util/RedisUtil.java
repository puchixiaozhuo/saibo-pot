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
import java.util.Arrays;
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
     * 自增操作
     * @param key 键
     * @return 自增后的值
     */
    public static Long incr(String key) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.incr(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        } finally {
            close(jedis);
        }
    }

    /**
     * 自减操作
     * @param key 键
     * @return 自减后的值
     */
    public static Long decr(String key) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.decr(key);
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

    /**
     * 设置键值对（仅当键不存在时）
     * @param key 键
     * @param value 值
     * @return true-设置成功, false-键已存在
     */
    public static Boolean setnx(String key, String value) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.setnx(key, value) == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            close(jedis);
        }
    }

    /**
     * ZSet: 添加元素
     * @param key 键
     * @param score 分数（排序依据）
     * @param member 成员
     * @return 是否添加成功
     */
    public static Boolean zadd(String key, double score, String member) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.zadd(key, score, member) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            close(jedis);
        }
    }

    /**
     * ZSet: 倒序获取指定范围的元素
     * @param key 键
     * @param start 起始索引
     * @param end 结束索引
     * @return 元素集合
     */
    public static java.util.List<String> zrevrange(String key, long start, long end) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.zrevrange(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        } finally {
            close(jedis);
        }
    }

    /**
     * ZSet: 统计指定分数范围内的元素数量
     * @param key 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素数量
     */
    public static Long zcount(String key, double min, double max) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.zcount(key, min, max);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        } finally {
            close(jedis);
        }
    }

    /**
     * ZSet: 删除指定排名范围的元素
     * @param key 键
     * @param start 起始排名
     * @param end 结束排名
     * @return 删除的元素数量
     */
    public static Long zremrangeByRank(String key, long start, long end) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.zremrangeByRank(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        } finally {
            close(jedis);
        }
    }

    /**
     * 执行 Lua 脚本（保证原子性操作）
     * @param script Lua 脚本
     * @param keys Redis键列表
     * @param args 参数列表
     * @return 执行结果
     */
    public static Object eval(String script, List<String> keys, List<String> args) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.eval(script, keys, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(jedis);
        }
    }

    /**
     * 执行 Lua 脚本（简化版 - 单个key）
     * @param script Lua 脚本
     * @param key Redis键
     * @param args 参数列表
     * @return 执行结果
     */
    public static Object eval(String script, String key, String... args) {
        List<String> keys = Arrays.asList(key);
        List<String> argList = Arrays.asList(args);
        return eval(script, keys, argList);
    }

    /**
     * Hash: 获取哈希表中指定字段的值
     * @param key 键
     * @param field 字段名
     * @return 字段值，不存在返回 null
     */
    public static String hget(String key, String field) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.hget(key, field);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(jedis);
        }
    }

    /**
     * Hash: 设置哈希表中字段的值
     * @param key 键
     * @param field 字段名
     * @param value 字段值
     * @return 操作结果
     */
    public static Long hset(String key, String field, String value) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.hset(key, field, value);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        } finally {
            close(jedis);
        }
    }

    /**
     * Hash: 批量设置哈希表字段
     * @param key 键
     * @param hash 字段-值映射
     * @return 操作结果
     */
    public static String hmset(String key, java.util.Map<String, String> hash) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.hmset(key, hash);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(jedis);
        }
    }

    /**
     * Hash: 批量获取哈希表字段
     * @param key 键
     * @param fields 字段名数组
     * @return 字段值列表
     */
    public static List<String> hmget(String key, String... fields) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.hmget(key, fields);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        } finally {
            close(jedis);
        }
    }

    /**
     * Hash: 删除哈希表中一个或多个字段
     * @param key 键
     * @param fields 字段名数组
     * @return 删除的字段数量
     */
    public static Long hdel(String key, String... fields) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.hdel(key, fields);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        } finally {
            close(jedis);
        }
    }

    /**
     * Hash: 判断哈希表中是否存在指定字段
     * @param key 键
     * @param field 字段名
     * @return true-存在，false-不存在
     */
    public static Boolean hexists(String key, String field) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.hexists(key, field);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            close(jedis);
        }
    }

    /**
     * Hash: 获取哈希表中所有字段和值
     * @param key 键
     * @return 字段-值映射
     */
    public static java.util.Map<String, String> hgetAll(String key) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            return jedis.hgetAll(key);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyMap();
        } finally {
            close(jedis);
        }
    }
}
