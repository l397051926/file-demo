package com.gennlife.fs.service.Part3Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.utils.HttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by wangyiyan on 2019/4/15
 */
@Component
public class AsyncQuery {
    private static Logger logger = LoggerFactory.getLogger(AsyncQuery.class);
    @Value("${part3.service.ES_PAGE_SIZE}")
    private int ES_PAGE_SIZE;
    @Value("${part3.service.LANG_JIA_PAGE_SIZE}")
    private int LANG_JIA_PAGE_SIZE;
    @Value("${urlbean.search_service_uri}")
    private String searchServerUrl;
    @Value("${urlbean.SearchIndexName}")
    private String indexName;
    @Autowired
    private HttpClient httpClient;

    @Async("taskExecutor")
    public Future<JsonObject> getPart3Url(String outGoingUrl, String taskId, String unumber, List<String> BBHList, int index){
        long start = System.currentTimeMillis();
        String result = "";
        JsonObject part3ParamObj = new JsonObject();
        part3ParamObj.addProperty("taskId", taskId);
        part3ParamObj.addProperty("unumber", unumber);
        part3ParamObj.addProperty("total", BBHList.size());
        part3ParamObj.addProperty("page", index+1);
        part3ParamObj.addProperty("page_size", LANG_JIA_PAGE_SIZE);

        JsonArray BBHArray = new JsonArray();
        int startIndex = index * LANG_JIA_PAGE_SIZE;
        int endIndex = (startIndex + LANG_JIA_PAGE_SIZE -1) > BBHList.size() - 1 ? BBHList.size() - 1 : (startIndex + LANG_JIA_PAGE_SIZE -1);
        for (int i = startIndex; i <= endIndex; i++){
            BBHArray.add(BBHList.get(i));
        }
        part3ParamObj.add("data", BBHArray);

        try {
            logger.info("第 " + (index+1) +"次请求朗珈服务，参数为：" + new Gson().toJson(part3ParamObj));
            result = httpClient.executPostWithBody(outGoingUrl, new Gson().toJson(part3ParamObj), 30000*60*60);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("第三方服务请求异常");
        }
        long end = System.currentTimeMillis();
        logger.info(index + " ==>Thread: " + Thread.currentThread().getName() + " ,获取朗珈返回链接完成时间为（"+(end - start) +"），结果为：" + result);
        return new AsyncResult<>(new JsonParser().parse(result).getAsJsonObject());
    }

    @Async("taskExecutor")//表示该方法允许异步调用
    public Future<List<JSONObject>> readSS2List(int index, List<String> patSnList, String searchServerUrl, JsonObject queryObj, String unumber, boolean isAdmin) {
        long start = System.currentTimeMillis();
        String searchResults = null;
        List<JSONObject> list = new ArrayList<>();
        int startIndex = index * ES_PAGE_SIZE;
        int endIndex = (startIndex + ES_PAGE_SIZE -1) > patSnList.size() - 1 ? patSnList.size() - 1 : (startIndex + ES_PAGE_SIZE -1);
        logger.info(startIndex + "  <===>  " + endIndex);
        String queryCondition = new Part3SampleServiceImpl().getProjectSearchConditon(unumber, startIndex, endIndex, patSnList, isAdmin);
        queryObj.addProperty("query", queryCondition);
        String queryStr = new Gson().toJson(queryObj);
        try {
            logger.info("请求es的参数为：" + queryStr);
            searchResults = httpClient.executPostWithBody(searchServerUrl, queryStr, 30000);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("请求异常");
        }
        JSONObject resultObj = JSONObject.parseObject(searchResults);
        JSONArray jsonArray = resultObj.getJSONObject("hits").getJSONArray("hits");
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(jsonArray.getJSONObject(i));
        }
        long end = System.currentTimeMillis();
        logger.info("==>Thread: " + Thread.currentThread().getName() + " ,获取患者基本信息和标本信息完成时间为（" + (end - start) + "）");
        return new AsyncResult<>(list);
    }

    /**
     * 从es获取到标本号
     * @param index 第几次请求
     * @param patSnList 患者编号（如果是我的项目申请，需要传患者编号，其余情况为空）
     * @param excludeArr 删除掉的样本号
     * @param paramObj uiservice请求参数
     * @return
     */
    @Async("taskExecutor")
    public Future<List<String>> getBBHListFromES(int index, List<String> patSnList, JSONArray excludeArr, JSONObject paramObj){
        List<String> BBHList = new ArrayList<>();
        String searchResults = null;
        QueryParam queryParam;

        String conditionParam = paramObj.getString("condition");
        String unumber = paramObj.getString("unumber");
        String source = paramObj.getString("source");
        boolean isAdmin = paramObj.getBoolean("isAdmin");

        //获取查询条件
        if (source.equals("myProject")){
            int startIndex = (index - 1) * ES_PAGE_SIZE;
            int endIndex = (startIndex + ES_PAGE_SIZE -1) > patSnList.size() - 1 ? patSnList.size() - 1 : (startIndex + ES_PAGE_SIZE -1);
            //得到高级搜索的条件
            String condition = new Part3SampleServiceImpl().getProjectSearchConditon(unumber, startIndex, endIndex, patSnList, isAdmin);
            queryParam = new Part3SampleServiceImpl().getQueryParam(condition, indexName);
        } else {
            queryParam = new Part3SampleServiceImpl().getQueryParam(conditionParam, indexName);
            queryParam.setSize(ES_PAGE_SIZE);
            queryParam.setPage(index);
        }

        long start = System.currentTimeMillis();
        try {
            logger.info("第 " + index +"次，请求es的参数：" + new Gson().toJson(queryParam));
            searchResults = httpClient.executPostWithBody(searchServerUrl, new Gson().toJson(queryParam), 30000);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("es第 "+ index +" 几次请求异常");
            logger.info("es异常信息：" + e.getMessage());
        }
        JSONObject results = JSONObject.parseObject(searchResults);

        //去除不需要的标本号
        List<String> pageList = new Part3SampleServiceImpl().getSampleBBH(results);
        for (int i =0; i < excludeArr.size(); i++){
            String str = excludeArr.getString(i);
            if (str.contains("-")){
                String[] split = str.split("-");
                for (int j = 0; j < split.length; j++){
                    if (pageList.contains(split[j])){
                        pageList.remove(split[j]);
                    }
                }
            } else {
                if (pageList.contains(str)){
                    pageList.remove(str);
                }
            }
        }
        if(pageList.size() > 0){
            BBHList.addAll(pageList);
        } else {
            logger.info("当前已经没有样本数据信息");
        }
        long end = System.currentTimeMillis();
        logger.info(index + " ==>Thread: " + Thread.currentThread().getName() + " ,获取标本号完成时间为（"+(end - start) +"ms）");
        return new AsyncResult<>(BBHList);
    }


    public String getSearchServerUrl() {
        return searchServerUrl;
    }

    public void setSearchServerUrl(String searchServerUrl) {
        this.searchServerUrl = searchServerUrl;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public int getES_PAGE_SIZE() {
        return ES_PAGE_SIZE;
    }

    public void setES_PAGE_SIZE(int ES_PAGE_SIZE) {
        this.ES_PAGE_SIZE = ES_PAGE_SIZE;
    }

    public int getLANG_JIA_PAGE_SIZE() {
        return LANG_JIA_PAGE_SIZE;
    }

    public void setLANG_JIA_PAGE_SIZE(int LANG_JIA_PAGE_SIZE) {
        this.LANG_JIA_PAGE_SIZE = LANG_JIA_PAGE_SIZE;
    }
}
