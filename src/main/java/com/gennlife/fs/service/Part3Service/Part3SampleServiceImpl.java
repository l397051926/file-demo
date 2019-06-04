package com.gennlife.fs.service.Part3Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.fs.common.dao.JedisClusterDao;
import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.utils.HttpClient;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.system.bean.SampleBean;
import com.gennlife.fs.system.bean.SampleResult;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by wangyiyan on 2019/4/8
 */
@Service
public class Part3SampleServiceImpl implements Part3SampleService {
    private static Logger logger = LoggerFactory.getLogger(Part3SampleServiceImpl.class);
    private static int indexKey = 1;
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
    private HttpClient httpClient;
    @Autowired
    private AsyncQuery asyncQuery;
    @Autowired
    private AsyncQueryTotal asyncQueryTotal;
    //获取到redis连接池
    private static JedisClusterDao jedisClusterDao = JedisClusterDao.getRedisDao();

    @Override
    public String confirmSpecInfos(String param) {
        String url = outGoingUrl;
        if(StringUtils.isEmpty(outGoingUrl)){
            throw new RuntimeException("没有配置样本库申请的url");
        }
        logger.info("申请出库的url ="+url);
        JSONObject paramObj = JSONObject.parseObject(param);
        List<String> BBHlist = new ArrayList<>();
        String result;
        try {
            String source = paramObj.getString("source");
            String taskId = paramObj.getString("taskId");
            int totalPatSn = paramObj.getIntValue("totalPatSn");
            JSONArray excludeArr = paramObj.getJSONArray("exclude");
            BlockingQueue<Future<List<String>>> queue = new LinkedBlockingQueue<>();

            if (source.equals("patientDetail") || source.equals("searchIndex")) {
                int n = (totalPatSn  % ES_PAGE_SIZE == 0) ? (totalPatSn / ES_PAGE_SIZE) : (totalPatSn / ES_PAGE_SIZE) + 1;
                for (int j = 1; j <= n; j++){
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Future<List<String>> future = asyncQuery.getBBHListFromES(j, new ArrayList<>(), excludeArr, paramObj);
                    queue.add(future);
                }
            } else {
                if (!jedisClusterDao.isExists(taskId)){
                    return wrapperMesage(0, "info", "当前taskId — " + taskId + " 不存在");
                }
                int n = (totalPatSn  % ES_PAGE_SIZE == 0) ? (totalPatSn / ES_PAGE_SIZE) : (totalPatSn / ES_PAGE_SIZE) + 1;
                List<String> patSnList = jedisClusterDao.getValue(taskId, 0, totalPatSn -1);
                for (int j = 1; j <= n; j++){
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Future<List<String>> future = asyncQuery.getBBHListFromES(j, patSnList, excludeArr, paramObj);
                    queue.add(future);
                }
            }

            int queueSize = queue.size();
            for (int i = 0; i < queueSize; i++) {
                List<String> list = null;
                try {
                    list = queue.take().get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!CollectionUtils.isEmpty(list)) {
                    BBHlist.addAll(list);
                }
            }
            result = getOpenUrlResult(paramObj, BBHlist);
            jedisClusterDao.del(taskId);
            jedisClusterDao.del(taskId + "-BBHSIZE");
            logger.info("返回前端的结果数据="+result);
        } catch (Exception e) {
            e.printStackTrace();
            result = wrapperMesage(0, "info","系统异常");
        }
        return result;
    }

    /**
     *
     * @param paramObj 参数
     * @param BBHList 标本号list
     * @return
     */
    private String getOpenUrlResult(JSONObject paramObj, List<String> BBHList) {
        String uid = paramObj.getString("uid");
        String pwd = paramObj.getString("pwd");
        String taskId = paramObj.getString("taskId");
        String unumber = paramObj.getString("unumber");
        int total = BBHList.size();
        String result = "";

        BlockingQueue<Future<JsonObject>> queue = new LinkedBlockingQueue<>();
        int n = (total % LANG_JIA_PAGE_SIZE == 0) ? (total / LANG_JIA_PAGE_SIZE) : (total / LANG_JIA_PAGE_SIZE) + 1;
        for (int j = 0; j < n; j++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Future<JsonObject> part3UrlObj = asyncQuery.getPart3Url(outGoingUrl, taskId, unumber, BBHList, j);
            queue.add(part3UrlObj);
        }

        int queueSize = queue.size();
        List<String> failBBH = new ArrayList<>();
        String url = "";
        for (int i = 0; i < queueSize; i++) {
            try {
                JsonObject obj = queue.take().get();
                JsonArray failArr = obj.get("failBBH").getAsJsonArray();
                for (int j = 0; j < failArr.size(); j++){
                    failBBH.add(failArr.get(j).getAsString());
                }
                url = obj.get("url").getAsString() + "?uid=" + uid + "&pwd=" + pwd;
            } catch (Exception e) {
                e.printStackTrace();
                return wrapperMesage(0, "info", "系统异常");
            }
        }

        if (!StringUtils.isEmpty(url)){
            result =  wrapperMesage(1, "url", url);
        } else if (failBBH.size() == total){
            result = wrapperMesage(0, "info", "第三方没有返回地址");
        }

        JsonObject object = new JsonParser().parse(result).getAsJsonObject();
        object.add("failBBH", new JsonParser().parse(failBBH.toString()).getAsJsonArray());
        return new Gson().toJson(object);
    }

    /**
     * 获取到可供选择的样本数据信息
     * @param paramObj 传递的参数对象
     * @param infosMap 存放展示的样本信息
     * @return
     */
    @Override
    public String applyOutGoing(JSONObject paramObj, ConcurrentHashMap<String,JSONObject> infosMap) {
        String result;
        String conditionParam = paramObj.getString("condition");
        String unumber = paramObj.getString("unumber");
        String taskId = paramObj.getString("taskId").trim();
        boolean isAdmin = paramObj.getBoolean("isAdmin");
        QueryParam queryParam = getQueryParam(conditionParam, indexName);

        //申请样本的地方一共为三个
        String source = paramObj.getString("source").trim();
        try {
            //从患者详情页搜索
            if (source.equals("patientDetail") || source.equals("searchIndex")){
                queryParam.setSize(paramObj.getIntValue("size"));
                queryParam.setPage(paramObj.getIntValue("page"));
                JsonObject object = JsonAttrUtil.toJsonObject(queryParam);
                String searchResults = httpClient.executPostWithBody(searchServerUrl, new Gson().toJson(object), 30000);
                JSONObject results = JSONObject.parseObject(searchResults);
                int totalPatSn = results.getJSONObject("hits").getIntValue("total");
                //获取标本号总数
                Future<Integer> totalBBHSize = asyncQueryTotal.getTotalBBH(new ArrayList<>() , totalPatSn, paramObj);
                List<SampleBean> list = getSampleResult(results);
                if (StringUtil.isEmptyStr(taskId)){
                    taskId = UUID.randomUUID().toString();
                }
                for (;;) {
                    if(totalBBHSize.isDone()) {
                        break;
                    }
                    Thread.sleep(10);
                }
                SampleResult sampleResult = new SampleResult(1, taskId, totalPatSn, totalBBHSize.get(), list);
                return new Gson().toJson(sampleResult);
            } else {
                long size = paramObj.getLongValue("size");
                long page = paramObj.getLongValue("page");
                if (StringUtil.isEmptyStr(taskId) || !jedisClusterDao.isExists(taskId)){
                    JSONArray patSnArr = paramObj.getJSONArray("patSns");
                    queryParam.setSize(ES_PAGE_SIZE);
                    queryParam.setPage(1);
                    JsonObject queryObj = JsonAttrUtil.toJsonObject(queryParam);
                    List<String> patSnList = (List<String>) JSONArray.parse(JSON.toJSONString(patSnArr));

                    long start1 = System.currentTimeMillis();
                    List<SampleBean> sampleBeanList = new ArrayList<>();

                    int BBHSIZE = getHasSpecPatienSn(unumber, queryObj, sampleBeanList, patSnList, isAdmin, (int)size);
                    long end1 = System.currentTimeMillis();
                    logger.info("获取含有标本信息的患者信息用时为：" + (end1 - start1));

                    //获取患者编号list并写入list
                    String task_uuid = UUID.randomUUID().toString();
                    Future<Boolean> patListTask = asyncQueryTotal.getPatSnList(patSnList, task_uuid);
                    logger.info("================获取患者标号总数，继续往下执行==============");


                    jedisClusterDao.putValue(task_uuid + "-BBHSIZE", BBHSIZE+"", 60*60*1000);
                    SampleResult sampleResult = new SampleResult(1, task_uuid, patSnList.size(), BBHSIZE, sampleBeanList);

                    for (;;) {
                        if(patListTask.isDone()) {
                            break;
                        }
                        Thread.sleep(10);
                    }

                    result = new Gson().toJson(sampleResult);
                    long end2 = System.currentTimeMillis();
                    logger.info("用时为：" + (end2 - end1));
                } else {
                    int listSize = jedisClusterDao.getListValueSize(taskId);
                    int totalBBHSize = Integer.parseInt(jedisClusterDao.getValue(taskId + "-BBHSIZE"));
                    int startIndex = (int)((page - 1) * size);
                    int endIndex = (startIndex + (int)size - 1) > listSize - 1 ? listSize - 1 : (startIndex + (int)size - 1);
                    logger.info(startIndex + "  <===>  " + endIndex);
                    //得到高级搜索的条件
                    List<String> patSnList = jedisClusterDao.getValue(taskId, startIndex, endIndex);
                    logger.info(patSnList.toString());
                    String queryCondition = getProjectSearchConditon(unumber, 0, patSnList.size()-1, patSnList, isAdmin);
                    //获取分页查询的数据
                    JsonObject queryObj = JsonAttrUtil.toJsonObject(queryParam);
                    List<SampleBean> tempList = getRedisDataByLimit(queryObj, queryCondition);
                    SampleResult sampleResult = new SampleResult(1, taskId, listSize, totalBBHSize, tempList);
                    result =  new Gson().toJson(sampleResult);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
            result =  wrapperMesage(0, "info","系统异常");
        }
        return result;
    }


    public QueryParam getQueryParam(String conditionParam, String indexName){
        QueryParam queryParam = new QueryParam();
        queryParam.setQuery(conditionParam);
        queryParam.setIndexName(indexName);
        queryParam.setHospitalID("public");
        queryParam.setTime_out(1000*60*5);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add("patient_info.PATIENT_SN");
        jsonArray.add("patient_info.GENDER");
        jsonArray.add("patient_info.BIRTH_DATE");
        jsonArray.add("patient_info.ETHNIC");
        jsonArray.add("patient_info.MARITAL_STATUS");
        jsonArray.add("specimen_info.SPECIMEN_SN");
        jsonArray.add("specimen_info.SPECIMEN_TYPE");
        jsonArray.add("specimen_info.SAMPLING_TIME");
        queryParam.addsource(jsonArray);
        return queryParam;
    }

    public String getProjectSearchConditon(String unumber, int start, int end, List<String> patSnList, boolean isAdmin){
        String queryStr = "(([患者基本信息.患者编号] 包含 ";
        for (int j = start; j <= end; j++){
            queryStr += patSnList.get(j) + ",";
        }
        queryStr = queryStr.substring(0, queryStr.lastIndexOf(","));
        if (isAdmin){
            queryStr = queryStr + ") AND [标本信息.标本号] EXIST TRUE)";
        } else {
            queryStr = queryStr + ") AND [标本信息.标本负责人] 包含 " + unumber + ")";
        }
        return queryStr;
    }

    /**
     * redis里面查询数据
     * @param queryObj es查询封装的参数
     * @param queryCondition es高级搜素条件
     * @return 返回封装好的需要展示的样本数据信息
     */
    private List<SampleBean> getRedisDataByLimit(JsonObject queryObj, String queryCondition){
        try {
            queryObj.addProperty("query", queryCondition);
            String searchResults = httpClient.executPostWithBody(searchServerUrl, new Gson().toJson(queryObj), 30000);
            JSONObject results = JSONObject.parseObject(searchResults);
            List<SampleBean> sampleList = getSampleResult(results);
            return sampleList;
        } catch (Exception e) {
            logger.info("请求es失败");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取有样本数据的患者编号
     * @param unumber 当前用户的工号
     * @param queryObj es查询条件
     * @param patSnList  待筛选的患者编号
     * @return 返回有样本数据与权限的患者编号
     */
    private int getHasSpecPatienSn(String unumber, JsonObject queryObj, List<SampleBean> sampleBeanList, List<String> patSnList, boolean isAdmin, int size) {
        int patSN_length = patSnList.size();
        int count = (patSN_length % ES_PAGE_SIZE) == 0 ? patSN_length/ES_PAGE_SIZE : (patSN_length/ES_PAGE_SIZE)+1;
        BlockingQueue<Future<List<JSONObject>>> queue = new LinkedBlockingQueue<>();
        for (int i = 0; i < count; i++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Future<List<JSONObject>> future = asyncQuery.readSS2List(i, patSnList, searchServerUrl, queryObj, unumber, isAdmin);
            queue.add(future);
        }

        int queueSize = queue.size();
        List<String> patSns = new ArrayList<>();
        int index = 0;
        int BBHSIZE = 0;
        for (int i = 0; i < queueSize; i++) {
            try {
                List<JSONObject> tempList = queue.take().get();
                for (JSONObject object : tempList){
                    JSONObject sourceObj = object.getJSONObject("_source");
                    if (index < size){
                        SampleBean sampleBean = wrappSampleInfos(sourceObj);
                        sampleBeanList.add(sampleBean);
                        index++;
                    }
                    patSns.add(object.getString("_id"));

                    //获取标本号总数
                    int specimen_info_size = sourceObj.getJSONArray("specimen_info").size();
                    BBHSIZE += specimen_info_size;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return BBHSIZE;
    }

    public String wrapperMesage(int code, String messageKey, String messageValue) {
        JSONObject object = new JSONObject();
        object.put("code",code);
        object.put(messageKey, messageValue);
        return object.toJSONString();
    }

    public List<SampleBean> getSampleResult(JSONObject results){
        indexKey = 1;
        List<SampleBean> list = new ArrayList<>();
        JSONObject hits = results.getJSONObject("hits");
        if(hits!=null&&hits.containsKey("hits")){
            JSONArray hits1 = hits.getJSONArray("hits");
            int size = hits1.size();
            if(hits1!=null && size >0){
                for (int i = 0; i<size;i++) {
                    JSONObject jsonObject = hits1.getJSONObject(i);
                    JSONObject sourceObj = jsonObject.getJSONObject("_source");
                    SampleBean sampleBean = wrappSampleInfos(sourceObj);
                    list.add(sampleBean);
                }
            }
        }
        return list;
    }

    public List<String> getSampleBBH(JSONObject results){
        List<String> list = new ArrayList<>();
        Set<String> quchongBBH = new HashSet();
        JSONObject hits = results.getJSONObject("hits");
        if(hits!=null&&hits.containsKey("hits")){
            JSONArray hits1 = hits.getJSONArray("hits");
            int size = hits1.size();
            if(hits1!=null&& size >0){
                for (int i = 0; i<size;i++) {
                    JSONObject jsonObject = hits1.getJSONObject(i);
                    JSONObject sourceObj = jsonObject.getJSONObject("_source");
                    JSONArray specimen_info_array = sourceObj.getJSONArray("specimen_info");
                    int specimen_size = specimen_info_array == null ? 0 : specimen_info_array.size();
                    for (int j = 0; j < specimen_size; j++) {
                        JSONObject specimenObj = specimen_info_array.getJSONObject(j);
                        String SPECIMEN_SN = specimenObj.getString("SPECIMEN_SN");
                        if (quchongBBH.add(SPECIMEN_SN)){
                            list.add(SPECIMEN_SN);
                        }
                    }
                }
            }
        }
        return list;
    }

    private SampleBean wrappSampleInfos(JSONObject source){
        JSONArray patient_info_array = source.getJSONArray("patient_info");
        JSONArray specimen_info_array = source.getJSONArray("specimen_info");
        String PATIENT_SN = patient_info_array.getJSONObject(0).getString("PATIENT_SN");
        String GENDER = patient_info_array.getJSONObject(0).getString("GENDER");
        String BIRTH_DATE = patient_info_array.getJSONObject(0).getString("BIRTH_DATE");
        String ETHNIC = patient_info_array.getJSONObject(0).getString("ETHNIC");
        String MARITAL_STATUS = patient_info_array.getJSONObject(0).getString("MARITAL_STATUS");

        SampleBean sampleBean = new SampleBean();
        sampleBean.setKey(indexKey);
        sampleBean.setPATIENT_SN(PATIENT_SN);
        sampleBean.setGENDER(GENDER);
        sampleBean.setBIRTH_DATE(BIRTH_DATE);
        sampleBean.setETHNIC(ETHNIC);
        sampleBean.setMARITAL_STATUS(MARITAL_STATUS);
        indexKey ++;

        String SPECIMEN_SN = "";
        String SPECIMEN_TYPE = "";
        String SAMPLING_TIME = "";
        int size = specimen_info_array == null ? 0 : specimen_info_array.size();
        for (int i = 0; i < size; i++) {
            JSONObject specimenObj = specimen_info_array.getJSONObject(i);
            SPECIMEN_SN +=   "-" + specimenObj.getString("SPECIMEN_SN");
            SPECIMEN_TYPE += "、" + specimenObj.getString("SPECIMEN_TYPE");
            SAMPLING_TIME +=  "、" + specimenObj.getString("SAMPLING_TIME");
        }
        sampleBean.setBbh(SPECIMEN_SN.substring(1, SPECIMEN_SN.length()));
        sampleBean.setSPECIMEN_TYPE(SPECIMEN_TYPE.substring(1, SPECIMEN_TYPE.length()));
        sampleBean.setSAMPLING_TIME(SAMPLING_TIME.substring(1, SAMPLING_TIME.length()));
        return sampleBean;
    }

    @Override
    public String encryptPWD(String param) {
        String resultStr = "";
        try {
            JsonObject jsonObject = new JsonParser().parse(param).getAsJsonObject();
            String rsapwd = jsonObject.get("RSAPWD").getAsString();
//            String orgPWD = RSAEncrypt.decrypt(rsapwd);
            JsonObject resultBean = new JsonObject();
            resultBean.addProperty("code", 1);
            resultBean.addProperty("data", rsapwd);
            resultStr = new Gson().toJson(resultBean);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return resultStr;
    }


    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getSearchServerUrl() {
        return searchServerUrl;
    }

    public void setSearchServerUrl(String searchServerUrl) {
        this.searchServerUrl = searchServerUrl;
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

