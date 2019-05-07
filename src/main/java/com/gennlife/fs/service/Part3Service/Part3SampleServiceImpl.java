/**
 * copyRight
 */
package com.gennlife.fs.service.Part3Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.utils.HttpClient;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.RSAEncrypt;
import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liuzhen
 * Created by liuzhen.
 * Date: 2018/10/10
 * Time: 11:13
 */
@Service
public class Part3SampleServiceImpl implements Part3SampleService {
    private static Logger logger = LoggerFactory.getLogger(Part3SampleServiceImpl.class);
    @Value("${part3.service.access_token_url}")
    private String accessTokenUrl;
    @Value("${part3.service.apply.outgoing.url}")
    private String outGoingUrl;
    @Value("#{${part3.service.access_token_meta}}")
    private Map<String,Object> tokenMeta;
    @Value("${urlbean.search_service_uri}")
    private String searchServerUrl;
    @Value("${vitark.sample.condition}")
    private String sampleCondition;
    @Value("${urlbean.SearchIndexName}")
    private String indexName;
    @Value("${part3.service.needed.access_token}")
    private boolean neededAccessToken;
    @Value("${urlbean.exportUri}")
    private String searchServerExportUrl;
    @Autowired
    private HttpClient httpClient;

    /**
     * 获取access token
     * @return
     */
    @Override
    public JsonObject ouath(){

        long start = System.currentTimeMillis();
        if(StringUtils.isEmpty(accessTokenUrl)&&neededAccessToken){
            throw new RuntimeException("配置错误,需要获取access_token,但是accessTokenUrl为空");
        }
        try {
            JsonObject object = new JsonObject();
            if (neededAccessToken) {

                Map<String,String> paramMap = new HashMap<>();
                String param = new Gson().toJson(tokenMeta);

                String result = httpClient.executPostWithBody(accessTokenUrl, param, 30000);
                logger.info("第三方系统返回数据为\t{}",result);
                object = JsonAttrUtil.toJsonObject(result);
                if(object.has("access_token")){
                    object.addProperty("status","200");
                }else{
                    object.addProperty("status","500");
                    object.addProperty("message","第三方接口返回数据错误");
                }
            }else{
                object.addProperty("status","200");
                object.addProperty("message","不需要获取token");
            }
            logger.info("从朗加系统获取access_token用时="+(System.currentTimeMillis()-start)+"ms");
            return object;
        } catch (Exception e) {
            return wrapperResult("", "第三方系统异常\t"+e);
        }
    }

    /**
     * 样本申请出库
     * @param param
     * @return
     */
    @Override
    public String applyOutGoing(String token, String param) {

        long start = System.currentTimeMillis();
        String url = outGoingUrl;
        if(StringUtils.isEmpty(outGoingUrl)){
            throw new RuntimeException("没有配置样本库申请的url");
        }
        logger.info("申请出库的url ="+url);

        JsonArray jsonArray = new JsonParser().parse(param).getAsJsonArray();
        JsonObject paramObj = new JsonObject();
        paramObj.addProperty("token",token);
        paramObj.add("data",jsonArray);

        String newParam = new Gson().toJson(paramObj);

        try {
            /**"{\"status\": \"success\",\"url\":\"http://10.0.0.180:10081/uranus/search_hit_details.html?\"}";//*/
            String result = httpClient.executPostWithBody(url, newParam, 30000);
            logger.info("第三方系统返回数据\t{}",result);
            JsonObject object = JsonAttrUtil.toJsonObject(result);
            String status = "";
            if(object != null){
                status = object.get("status").getAsString();
            }
            if(StringUtils.equalsIgnoreCase(status,"success")){
                object.addProperty("status","200");
                object.addProperty("token",token);
            }else{
                object.addProperty("status","400");
                object.addProperty("message","第三方系统异常");
            }
            String results = object.toString();
            logger.info("返回前端的结果数据="+results);
            logger.info("申请出库用时="+(System.currentTimeMillis()-start)+"ms");
            return results;
        } catch (Exception e) {
            return wrapperResult("", "第三方系统异常\t"+e).getAsString();
        }
    }

    /**
     * 获取所有的样本编号
     * @param param
     * @return
     */
    @Override
    public void getSampleNumber(String param, ConcurrentHashMap<String,JSONObject> result) {
        JSONArray sampleNums = new JSONArray();
        long start = System.currentTimeMillis();
        if(StringUtils.isEmpty(param)){
            logger.warn("param is null");
            return ;
        }
        param = param.concat(" AND ([标本信息.标本号] EXIST TRUE)");
        QueryParam queryParam = new QueryParam();
        queryParam.setQuery(param);
        queryParam.setIndexName(indexName);
        queryParam.setHospitalID("public");
        queryParam.addsource("specimen_info.SPECIMEN_SN");
        queryParam.addsource("patient_info");
        queryParam.setTime_out(1000*60*5);
        queryParam.setSize(1000);
        JsonObject object = JsonAttrUtil.toJsonObject(queryParam);
        try {
            String asString = object.toString();
            String searchResult = httpClient.executPostWithBody(searchServerExportUrl, asString, 30000);
            JSONObject results = JSONObject.parseObject(searchResult);
            if(getSearchResult(results,"specimen_info.SPECIMEN_SN",result)){
                String scroll_id = results.getString("_scroll_id");
                while (true) {
                    object.addProperty("_scroll_id",scroll_id);
                    searchResult = httpClient.executPostWithBody(searchServerExportUrl, object.toString(),30000);
                    results = JSONObject.parseObject(searchResult);
                    if(!getSearchResult(results,"specimen_info.SPECIMEN_SN",result)){
                        break;
                    }
                    scroll_id = results.getString("_scroll_id");
                }
            }
        } catch (Exception e) {
            logger.error("获取样本信息失败:");
            return ;
        }
        logger.info("获取所有的样本编号用时="+(System.currentTimeMillis()-start)+"ms");
    }

    @Override
    public String encryptPWD(String param) {
        String resultStr = "";
        try {
            JsonObject jsonObject = new JsonParser().parse(param).getAsJsonObject();
            String rsapwd = jsonObject.get("RSAPWD").getAsString();
            String orgPWD = RSAEncrypt.decrypt(rsapwd);
            JsonObject resultBean = new JsonObject();
            resultBean.addProperty("code", 1);
            resultBean.addProperty("data", orgPWD);
            resultStr = new Gson().toJson(resultBean);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return resultStr;
    }


    private boolean getSearchResult(JSONObject results, String key, ConcurrentHashMap<String,JSONObject> result){
        JSONObject hits = results.getJSONObject("hits");
        boolean falg = false;
        if(hits!=null&&hits.containsKey("hits")){
            JSONArray hits1 = hits.getJSONArray("hits");
            int size = hits1.size();
            if(hits1!=null&& size >0){
                falg = true;
                for (int i = 0; i<size;i++) {
                    JSONObject jsonObject = hits1.getJSONObject(i);
                    JSONObject source = jsonObject.getJSONObject("_source");
                    wrappSampleNum(source,key,result);
                }
            }
        }
        return falg;
    }

    /**
     * 搜索结果，获取所有的样本号
     * @param source
     * @param key
     */
    private void wrappSampleNum(JSONObject source, String key,ConcurrentHashMap<String,JSONObject> result){
        String lastProperty = StringUtils.substringAfterLast(key,".");
        String firstProperty = StringUtils.substringBefore(key,".");
        JSONArray first = source.getJSONArray(firstProperty);
        int size = first == null ? 0 : first.size();
        for (int i = 0; i < size; i++) {
            JSONObject jsonObject = first.getJSONObject(i);
            String value = jsonObject.getString(lastProperty);
            SampleNumber number = new SampleNumber(value);
            JSONObject jsonValue = (JSONObject) JSON.toJSON(number);
            if(!result.containsKey(value)){
                result.put(value,jsonValue);
            }
        }
    }
    private class SampleNumber{
        private String hzxm = "";
        private String nl = "";
        private String xb = "";
        private String zy = "";
        private String mz = "";
        private String zyh = "";
        private String bbh = "";
        private String ybh = "";

        public SampleNumber(String bbh) {
            this.bbh = bbh;
        }

        public SampleNumber(String bbh, String ybh) {

            this.bbh = bbh;
            this.ybh = ybh;
        }

        public String getHzxm() {

            return hzxm;
        }

        public void setHzxm(String hzxm) {

            this.hzxm = hzxm;
        }

        public String getNl() {

            return nl;
        }

        public void setNl(String nl) {

            this.nl = nl;
        }

        public String getXb() {

            return xb;
        }

        public void setXb(String xb) {

            this.xb = xb;
        }

        public String getZy() {

            return zy;
        }

        public void setZy(String zy) {

            this.zy = zy;
        }

        public String getMz() {

            return mz;
        }

        public void setMz(String mz) {

            this.mz = mz;
        }

        public String getZyh() {

            return zyh;
        }

        public void setZyh(String zyh) {

            this.zyh = zyh;
        }

        public String getBbh() {

            return bbh;
        }

        public void setBbh(String bbh) {

            this.bbh = bbh;
        }

        public String getYbh() {

            return ybh;
        }

        public void setYbh(String ybh) {

            this.ybh = ybh;
        }
    }

    private JsonObject wrapperResult(String key, String message) {

        JsonObject object = new JsonObject();
        object.addProperty("status","500");
        object.addProperty("message",message);
        logger.info("获取地方系统授权信息异常,key={},message={}",key,message);
        return object;
    }

    public String getAccessTokenUrl() {

        return accessTokenUrl;
    }

    public void setAccessTokenUrl(String accessTokenUrl) {

        this.accessTokenUrl = accessTokenUrl;
    }

    public String getOutGoingUrl() {

        return outGoingUrl;
    }

    public void setOutGoingUrl(String outGoingUrl) {

        this.outGoingUrl = outGoingUrl;
    }

    public Map<String, Object> getTokenMeta() {

        return tokenMeta;
    }

    public void setTokenMeta(Map<String, Object> tokenMeta) {

        this.tokenMeta = tokenMeta;
    }

    public String getSearchServerUrl() {

        return searchServerUrl;
    }

    public void setSearchServerUrl(String searchServerUrl) {

        this.searchServerUrl = searchServerUrl;
    }

    public String getSampleCondition() {

        return sampleCondition;
    }

    public void setSampleCondition(String sampleCondition) {

        this.sampleCondition = sampleCondition;
    }

    public String getIndexName() {

        return indexName;
    }

    public void setIndexName(String indexName) {

        this.indexName = indexName;
    }

    public boolean isNeededAccessToken() {

        return neededAccessToken;
    }

    public void setNeededAccessToken(boolean neededAccessToken) {

        this.neededAccessToken = neededAccessToken;
    }

    public String getSearchServerExportUrl() {

        return searchServerExportUrl;
    }

    public void setSearchServerExportUrl(String searchServerExportUrl) {

        this.searchServerExportUrl = searchServerExportUrl;
    }
}
