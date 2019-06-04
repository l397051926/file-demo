package com.gennlife.fs.common.dao;

import com.gennlife.fs.common.cache.CacheDAOInterface;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Scope("singleton")
@ConfigurationProperties(prefix = "fs.redis.config")
public class JedisClusterDao implements FactoryBean<JedisClusterDao>, InitializingBean, CacheDAOInterface<String>, DisposableBean {
    @Autowired
    private GenericObjectPoolConfig genericObjectPoolConfig;
    private static JedisCluster jedisCluster;
    private int connectionTimeout = 2000;
    private static Logger logger = LoggerFactory.getLogger(JedisClusterDao.class);
    private int soTimeout = 3000;
    private int maxRedirections = 5;
    private String jedisClusterNodes;
    private static final String PREFIX = "{FileService}-";
    private final int EXPIRETIME = 3 * 60;
    private static JedisClusterDao jedisClusterDao = null;

    private JedisClusterDao() {
        jedisClusterDao = this;
    }

    public static JedisClusterDao getRedisDao() {
        return jedisClusterDao;
    }

    @Override
    public JedisClusterDao getObject() {
        return this;
    }

    @Override
    public Class<?> getObjectType() {
        return (this.jedisCluster != null ? this.getClass() : JedisClusterDao.class);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (jedisClusterNodes == null || jedisClusterNodes.length() == 0) {
            throw new NullPointerException("jedisClusterNodes is null.");
        }
        Set<HostAndPort> haps = new HashSet<HostAndPort>();
        for (String node : jedisClusterNodes.split(";")) {
            String[] arr = node.split(":");
            if (arr.length != 2) {
                throw new ParseException("node address error !", node.length() - 1);
            }
            haps.add(new HostAndPort(arr[0], Integer.valueOf(arr[1])));
        }

        jedisCluster = new JedisCluster(haps, connectionTimeout, soTimeout, maxRedirections, genericObjectPoolConfig);
    }

    public GenericObjectPoolConfig getGenericObjectPoolConfig() {
        return genericObjectPoolConfig;
    }

    public void setGenericObjectPoolConfig(GenericObjectPoolConfig genericObjectPoolConfig) {
        this.genericObjectPoolConfig = genericObjectPoolConfig;
    }

    public JedisCluster getJedisCluster() {
        return jedisCluster;
    }

    public void setJedisCluster(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public int getMaxRedirections() {
        return maxRedirections;
    }

    public void setMaxRedirections(int maxRedirections) {
        this.maxRedirections = maxRedirections;
    }

    public String getJedisClusterNodes() {
        return jedisClusterNodes;
    }

    public void setJedisClusterNodes(String jedisClusterNodes) {
        this.jedisClusterNodes = jedisClusterNodes;
    }

    private void updateExpire(String key) {
        try {
            if (jedisCluster == null) return;
            if (jedisCluster.exists(key)) {
                Long expire = jedisCluster.ttl(key);
                Long result = jedisCluster.expire(key, expire.intValue() + EXPIRETIME);
                logger.info("read old expire time: key " + key + " TTL " + expire + " => new TTL " + result);
            }
        } catch (Exception e) {
            logger.error("redis error ", e);
        }
    }

    private static String BuildKey(String key) {
        return PREFIX + key;
    }

    public void putValue(String key, String value) {
        try {
            if (jedisCluster == null) return;
            key = BuildKey(key);
            jedisCluster.set(key, value);
            jedisCluster.expire(key, EXPIRETIME);
        } catch (Exception e) {
            logger.error("redis error ", e);
        }
    }

    public boolean putValue(String key, String value, long sec) throws Exception {
        try {
            if (jedisCluster == null) return false;
            key = BuildKey(key);
            jedisCluster.set(key, value);
            jedisCluster.expire(key, (int) sec);
            return true;
        } catch (Exception e) {
            logger.error("redis error ", e);
            return false;
        }
    }

    public String getValue(String key) throws Exception {
        if (jedisCluster == null) return null;
        try {
            String result = null;
            key = BuildKey(key);
            result = jedisCluster.get(key);
            return result;
        } catch (Exception e) {
            logger.error("redis error ", e);
            return null;
        }
    }

    public boolean putValue(String key, List<String> list, long sec){
        String[] strArr = new String[list.size()];
        list.toArray(strArr);
        try {
            if (jedisCluster == null) return false;
            key = BuildKey(key);
            jedisCluster.lpush(key, strArr);
            jedisCluster.expire(key, (int) sec);
            return true;
        } catch (Exception e) {
            logger.error("redis error ", e);
            return false;
        }
    }

    public List<String> getValue(String key, int start, int end){
        if (jedisCluster == null) return null;
        try {
            key = BuildKey(key);
            List<String> list = jedisCluster.lrange(key, start, end);
            return list;
        } catch (Exception e) {
            logger.error("redis error ", e);
            return null;
        }
    }

    public int getListValueSize(String key){
        if (jedisCluster == null) return 0;
        long llen = 0;
        try {
            key = BuildKey(key);
            llen = jedisCluster.llen(key);
        } catch (Exception e) {
            logger.error("redis error ", e);
        }
        return (int)llen;
    }

    @Override
    public boolean expire(String key, long expireTime) {
        try {
            key = BuildKey(key);
            return jedisCluster.expire(key, (int) expireTime) > 0;
        } catch (Exception e) {
            logger.error("", e);
            return false;
        }
    }

    public void del(String key) {
        try {
            if (jedisCluster == null) return;
            key = BuildKey(key);
            jedisCluster.del(key);
            logger.info("remove " + key);
        } catch (Exception e) {
            logger.error("redis error ", e);
        }
    }

    public void cleanAll() {
        if (jedisCluster == null) return;
        try {
            Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
            //遍历节点 获取所有符合条件的KEY
            for (String k : clusterNodes.keySet()) {
                JedisPool jp = clusterNodes.get(k);
                Jedis connection = jp.getResource();
                try {
                    for (String key : connection.keys(BuildKey("*"))) {
                        jedisCluster.del(key);
                        logger.info("remove " + key);
                    }
                } catch (Exception e) {
                    logger.error("Getting keys error: {}", e);
                } finally {
                    connection.close();//用完一定要close这个链接！！！
                }
            }
        } catch (Exception e) {
            logger.error("redis error ", e);
        }
    }

    public String getValueFixed(String key) {
        if (jedisCluster == null) return null;
        try {
            key = BuildKey(key);
            String result = jedisCluster.get(key);
            return result;
        } catch (Exception e) {
            logger.error("redis error ", e);
            return null;
        }
    }

    @Bean
    @ConfigurationProperties(prefix = "fs.redis.config.pool")
    public GenericObjectPoolConfig createGenericObjectPoolConfig() {
        return new GenericObjectPoolConfig();
    }

    @Override
    public void destroy() throws Exception {
        cleanAll();
        jedisCluster.close();
    }

    public Map<String, String> hgetAll(String key) {
        try {
            key = BuildKey(key);
            return jedisCluster.hgetAll(key);
        } catch (Exception e) {
            logger.error("redis error ", e);
            return null;
        }
    }

    public String hget(String key, String field) {
        try {
            key = BuildKey(key);
            return jedisCluster.hget(key, field);
        } catch (Exception e) {
            logger.error("redis error ", e);
            return null;
        }
    }

    public boolean hset(String key, String field, String value, long time) {
        try {
            key = BuildKey(key);
            jedisCluster.hset(key, field, value);
            expire(key, time);
            return true;
        } catch (Exception e) {
            logger.error("redis error ", e);
            return false;
        }
    }

    public boolean hmset(String key, Map<String, String> map, long time) {
        try {
            key = BuildKey(key);
            jedisCluster.hmset(key, map);
            expire(key, time);
            return true;
        } catch (Exception e) {
            logger.error("redis error ", e);
            return false;
        }
    }

    public boolean isExists(String key) {
        try {
            key = BuildKey(key);
            return jedisCluster.exists(key);
        } catch (Exception e) {
            logger.error("", e);
            return false;
        }
    }

}