package com.gennlife.fs.service.cache;

import com.gennlife.fs.common.cache.CacheDAOInterface;
import com.gennlife.fs.common.cache.HttpRequestCacheABS;
import com.gennlife.fs.common.cache.MemLocalcacheService;
import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.StringUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;

/**
 * Created by Chenjinfeng on 2017/12/26.
 */
public class OnePatAccessCache extends HttpRequestCacheABS {
    private static final JsonObject FAIL = new JsonObject();
    private static final JsonObject SUCCESS = new JsonObject();
    public static final String ACCESS_KEY = "match";
    private static OnePatAccessCache onePatAccessCache = new OnePatAccessCache();

    public static OnePatAccessCache getInstance() {
        return onePatAccessCache;
    }

    static {
        FAIL.addProperty(ACCESS_KEY, false);
        SUCCESS.addProperty(ACCESS_KEY, true);
    }

    public JsonObject getData(String patient, QueryParam qp) {
        return getCacheDataByUpdate(getCacheKey(patient, qp), MemLocalcacheService.getMemcacheObj(), 1200, qp);
    }

    private String getCacheKey(String patient, QueryParam qp) {
        return "ACCESS_" + patient + "_" + StringUtil.bytesToMD5(qp.getRoleInfoStr().getBytes());
    }

    public static boolean hasAccess(String patient, QueryParam qp) {
        JsonObject access = onePatAccessCache.getData(patient, qp);
        return access.get(OnePatAccessCache.ACCESS_KEY).getAsBoolean();
    }

    public static boolean hasAccess(String patient, JsonObject param_json) {
        QueryParam qp = new QueryParam(param_json);
        qp.query_patient_sn(patient);
        return hasAccess(patient, qp);
    }

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(OnePatAccessCache.class);

    @Override
    protected JsonObject saveCache(CacheDAOInterface cacheDAO, String cacheKey, QueryParam qp) {
        JsonArray source = qp.getSource();
        qp.cleanSource();
        qp.addsource("patient_info.PATIENT_SN");
        qp.ignoreSearchOnePat();
        QueryResult result = HttpRequestUtils.search(qp);
        if (result.isHasError()) {
            return FAIL;
        }
        long total = result.getTotal();
        qp.setSearchOnePat();
        JsonObject value = null;
        if (total > 0) {
            qp.addsource(source);
            value = SUCCESS;
        } else {
            value = FAIL;
        }
        try {
            cacheDAO.putValue(cacheKey, value, 1200);
        } catch (Exception e) {
            logger.error("", e);
        }
        return value;
    }
}
