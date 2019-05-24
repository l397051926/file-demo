package com.gennlife.fs.common.cache;

import com.gennlife.fs.common.utils.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Chenjinfeng on 2017/1/4.
 */
public class MemLocalcacheService extends Thread implements CacheDAOInterface<Object> {
    private static final Logger logger = LoggerFactory.getLogger(MemLocalcacheService.class);
    private static final MemLocalcacheService MEM_LOCALCACHE_SERVICE = new MemLocalcacheService();
    private boolean isRun = false;
    private double leftMemory = 0;//预留的内存
    private ConcurrentHashMap<String, CacheEntry> map;
    private ConcurrentHashMap<String, CacheEntry> backmap;
    private long cycleTime = 5 * 1000;
    private boolean isCleaning = false;

    public static MemLocalcacheService getMemcacheObj() {
        return MEM_LOCALCACHE_SERVICE;
    }

    private MemLocalcacheService() {
        isRun = false;
        map = new ConcurrentHashMap<String, CacheEntry>();
    }

    @Override
    public Object getValue(String key) throws Exception {
        CacheEntry tmp = map.get(key);
        if (tmp == null && backmap != null && backmap.size() > 0)
            tmp = backmap.get(key);
        if (tmp == null) return null;
        return tmp.getData();
    }

    @Override
    /***
     * @param expiretime 单位秒
     * */
    public boolean putValue(String key, Object value, long expireTime) throws Exception {
        if (value == null || key == null) return false;
        if (map.contains(key)) return false;
        if (expireTime <= 0) expireTime = 10;
        CacheEntry newEntry = new CacheEntry(value, expireTime * 1000 + System.currentTimeMillis());
        CacheEntry oldEntry = null;
        if (hasEnoughMemorySerious()) {
            map.clear();
            logger.warn("no have enough data map clear");
        }
        if (isCleaning) {
            if (backmap != null) {
                oldEntry = backmap.putIfAbsent(key, newEntry);
            } else {
                logger.error("backmap empty");
                return false;
            }
        } else {
            oldEntry = map.putIfAbsent(key, newEntry);
        }

        if (oldEntry != null) {
            return false;
        }
        logger.info("save cache " + key + " in local memmory " + expireTime * 1000 + " ms");
        return true;
    }

    @Override
    public boolean expire(String key, long expireTime) {
        if (key == null) return false;
        if (map.contains(key)) return false;
        CacheEntry result = map.get(key);
        if (result == null) return false;
        if (expireTime <= 0) {
            map.remove(key);
            return true;
        }
        expireTime = expireTime * 1000 + System.currentTimeMillis();
        result.rsetExpire(expireTime);
        return true;
    }

    @Override
    public void del(String key) throws Exception {
        if (isCleaning) {
            CacheEntry tmp = map.get(key);
            if (tmp != null) tmp.setExpire();
        } else {
            map.remove(key);
        }
    }

    @Override
    public void run() {
//        logger.debug("start === cycleTime " + cycleTime + " ms");
        while (isRun) {
//            if (map.size() > 0) logger.debug("map size " + map.size());
            try {
                Thread.sleep(cycleTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            clean(!hasEnoughMemory());

        }
        logger.info("stop local Memcache");
    }

    public boolean hasEnoughMemory() {
        return Runtime.getRuntime().freeMemory() > leftMemory;
    }

    public boolean hasEnoughMemorySerious() {
        return Runtime.getRuntime().freeMemory() <= leftMemory * 0.5;
    }

    public void clean(boolean needgc) {
        if (isCleaning) return;
        if (backmap == null) backmap = new ConcurrentHashMap<>();
        if (map.size() == 0 && backmap.size() == 0) return;
        long s = map.size();
        isCleaning = true;
        LinkedList<String> cleanList = new LinkedList<>();
        long current = System.currentTimeMillis();
        if (needgc) current = current - 60000;//内存紧张
        for (String key : map.keySet()) {
            CacheEntry tmp = map.get(key);
            if (tmp != null) {
                if (tmp.isExpire(current)) {
                    cleanList.add(key);
                }
            }
        }
        for (String key : cleanList) {
            map.remove(key);
        }
        isCleaning = false;
        map.putAll(backmap);
        backmap.clear();
        long e = map.size();
        if (needgc) System.gc();
//        if (e != 0 || s != 0) logger.debug("clean  map  size start => " + s + " end=> " + e);
    }

    @Override
    public synchronized void start() {
        isRun = true;
        isCleaning = false;
        leftMemory = Runtime.getRuntime().freeMemory() * 0.2;
        if (leftMemory < 30 * 1024 * 1024) leftMemory = 30 * 1024 * 1024;
        logger.info("left memory " + NumberUtil.countSize((long) leftMemory));
        super.start();
    }

    public void stopService() {
        isRun = false;
        map.clear();
        if (backmap != null) backmap.clear();
    }

}

class CacheEntry {
    private long expire;
    private Object data;

    public CacheEntry(Object data, long expire) {
        this.data = data;
        this.expire = expire;
    }

    public Object getData() {
        return data;
    }

    public boolean isExpire(long nowTime) {
        return expire <= nowTime;
    }

    public void setExpire() {
        this.expire = 0;
    }

    public long getExpire() {
        return expire;
    }

    public void rsetExpire(long expire) {
        this.expire = expire;
    }
}
