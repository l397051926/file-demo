package com.gennlife.fs.common.cache;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Chenjinfeng on 2016/12/27.
 */
public abstract class HttpRequestCacheABS {
    private static ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<String, Object>();
    private static Logger logger = LoggerFactory.getLogger(HttpRequestCacheABS.class);
    private static MemLocalcacheService memLocalcacheService = MemLocalcacheService.getMemcacheObj();

    public  final JsonObject getCacheDataByUpdate(final String cacheKey, final CacheDAOInterface cacheDAO, int updateTime,final QueryParam qp) {
        return getCacheData(cacheKey, cacheDAO, qp, updateTime, true);
    }

    public final JsonObject getCacheData(final String cacheKey, final CacheDAOInterface cacheDAO, final QueryParam qp) {
        return getCacheData(cacheKey, cacheDAO, qp, 5);
    }

    public final JsonObject getCacheData(final String cacheKey, final CacheDAOInterface cacheDAO, final QueryParam qp, int sec) {
        return getCacheData(cacheKey, cacheDAO, qp, sec, false);
    }

    public final JsonObject getCacheData(final String cacheKey, final CacheDAOInterface cacheDAO, final QueryParam qp, int sec, boolean uptime) {
        Object obj = null;
        JsonObject result = null;
        result = getCacheJson(cacheKey, cacheDAO, sec);
        if (result != null) {
            if (uptime) {
                try {
                    cacheDAO.expire(cacheKey, sec);
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
            return result;
        }
        obj = map.get(cacheKey);
        if (obj == null) {
            obj = new Object();
            Object old = map.putIfAbsent(cacheKey, obj);
            if (old != null) obj = old;
        }
        synchronized (obj) {
            result = getCacheJson(cacheKey, cacheDAO, sec);
            if (result == null) {
                result = saveCache(cacheDAO, cacheKey, qp);
            }
            map.remove(cacheKey);
        }

        return result;
    }

    /**
     * expireTime 单位秒
     */
    protected void saveCacheInLocalMemmory(String key, Object value, long expireTime) {
        try {
            memLocalcacheService.putValue(key, value, expireTime);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private JsonObject getCacheJson(String cacheKey, CacheDAOInterface<String> cacheDAO, int sec) {
        //
        try {
            Object tmp = memLocalcacheService.getValue(cacheKey);
            if (tmp != null) {
                logger.info("read from local mem " + cacheKey);
                return (JsonObject) tmp;
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        //
        String cacheValue = null;
        try {
            if (cacheDAO != null) {
                cacheValue = cacheDAO.getValue(cacheKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
            cacheValue = null;
        }
        if (!StringUtil.isEmptyStr(cacheValue)) {
            try {
                memLocalcacheService.putValue(cacheKey, cacheValue, sec);

            } catch (Exception e) {
                logger.error("", e);
            }
            //logger.info(cacheKey.hashCode()+" read from cache");
            return JsonAttrUtil.toJsonObject(cacheValue);
        }
        return null;
    }

    protected abstract JsonObject saveCache(CacheDAOInterface cacheDAO, String cacheKey, QueryParam qp);
}
