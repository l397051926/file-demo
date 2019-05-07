package com.gennlife.fs.common.cache;

import com.gennlife.fs.common.model.QueryParam;
import com.google.gson.JsonObject;
/**
 * Created by Chenjinfeng on 2017/1/6.
 */
public class MergeRequestInLocal extends HttpRequestCacheABS {
    private MergeRequestModelInterface model;

    public MergeRequestInLocal(MergeRequestModelInterface model) {
        this.model = model;
    }

    public JsonObject getData()
    {
        return getCacheData(model.getKey(),null,null);
    }
    @Override
    protected JsonObject saveCache(CacheDAOInterface cacheDAO, String cacheKey, QueryParam qp) {
        JsonObject value=model.getValue();
        saveCacheInLocalMemmory(cacheKey,value,5);
        return value;
    }
}
