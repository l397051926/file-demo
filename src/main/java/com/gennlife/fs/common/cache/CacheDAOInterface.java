package com.gennlife.fs.common.cache;

/**
 * Created by Chenjinfeng on 2016/12/27.
 */
public interface CacheDAOInterface<T> {
    T getValue(String key) throws Exception;

    boolean putValue(String key, T value, long expireTime) throws Exception;

    void del(String key) throws Exception;

    default boolean expire(String key, long expireTime) {
        return true;
    }
}
