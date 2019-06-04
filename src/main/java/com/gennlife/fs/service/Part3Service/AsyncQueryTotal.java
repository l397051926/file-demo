package com.gennlife.fs.service.Part3Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.fs.common.dao.JedisClusterDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by wangyiyan on 2019/5/9
 */
@Component
public class AsyncQueryTotal {
    private static Logger logger = LoggerFactory.getLogger(AsyncQueryTotal.class);
    @Value("${part3.service.ES_PAGE_SIZE}")
    private int ES_PAGE_SIZE;
    @Value("${part3.service.LANG_JIA_PAGE_SIZE}")
    private int LANG_JIA_PAGE_SIZE;
    @Value("${part3.service.apply.outgoing.url}")
    private String outGoingUrl;
    @Value("${vitark.sample.condition}")
    private String sampleCondition;
    @Value("${urlbean.search_service_uri}")
    private String searchServerUrl;
    @Value("${urlbean.SearchIndexName}")
    private String indexName;

    @Autowired
    private AsyncQuery asyncQuery;
    //获取到redis连接池
    private static JedisClusterDao jedisClusterDao = JedisClusterDao.getRedisDao();

    /**
     * 获取标本号
     * @param patSnList 患者编号数组
     * @param totalPatSn 患者总数
     * @param paramObj uiservice传递参数
     * @return
     */
    @Async("taskExecutor")
    public Future<Integer> getTotalBBH(List<String> patSnList, int totalPatSn, JSONObject paramObj) {
        long start = System.currentTimeMillis();
        BlockingQueue<Future<List<String>>> queue = new LinkedBlockingQueue<>();
        int n = (totalPatSn  % ES_PAGE_SIZE == 0) ? (totalPatSn / ES_PAGE_SIZE) : (totalPatSn / ES_PAGE_SIZE) + 1;
        for (int j = 1; j <= n; j++){
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Future<List<String>> future = asyncQuery.getBBHListFromES(j, patSnList, new JSONArray(), paramObj);
            queue.add(future);
        }

        int queueSize = queue.size();
        int total = 0;
        for (int i = 0; i < queueSize; i++) {
            List<String> list = null;
            try {
                list = queue.take().get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!CollectionUtils.isEmpty(list)) {
                total += list.size();
            }
        }
        long end = System.currentTimeMillis();
        logger.info("获取样本总个数时间为：" + (end - start) + "ms");
        logger.info(" ==>Thread: " + Thread.currentThread().getName() + " ,获取样本数量（"+(end - start) +"ms）");
        logger.info("==================样本数量===================");
        return new AsyncResult<>(total);
    }


    @Async("taskExecutor")
    public Future<Boolean> getPatSnList(List<String> dataList, String task_uuid) {
        long start1 = System.currentTimeMillis();
        jedisClusterDao.putValue(task_uuid, dataList, 60*60*1000);
        long end1 = System.currentTimeMillis();
        logger.info("redis写入" + (end1-start1));
        return new AsyncResult<>(true);
    }
}
