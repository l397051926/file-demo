/**
 * copyRight
 */
package com.gennlife.fs.service.empi;

import com.gennlife.fs.common.dao.JedisClusterDao;
import com.gennlife.fs.common.utils.HttpClient;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class EMPIServiceImpl implements EMPIService {
    @Value("${fs.empi.server.url}")
    private String EMPIServerUrl;
    @Value("${fs.writeServiceUrl}")
    private String writeServiceUrl;
    private Logger logger = LoggerFactory.getLogger(EMPIServiceImpl.class);
    @Autowired
    private HttpClient httpClient;
    //获取到redis连接池
    private static JedisClusterDao jedisClusterDao = JedisClusterDao.getRedisDao();
    //创建线程池
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);

    @Override
    public String getEMPIInfo(String param) {
        logger.info("url = {},param={}",EMPIServerUrl,param);
        try {
            JsonObject json = JsonAttrUtil.toJsonElement(param).getAsJsonObject();
            String inPatientSn = JsonAttrUtil.getStringValue("InPatientSn", json);
            String idCard = JsonAttrUtil.getStringValue("IDCard",json);
            try {
                if (!inPatientSn.trim().equals("") || !idCard.trim().equals("")){
                    JsonObject oldObj = new JsonParser().parse(param).getAsJsonObject();
                    JsonObject newObj = new JsonObject();
                    newObj.addProperty("IDCard",oldObj.has("IDCard") ? oldObj.get("IDCard").getAsString() : "");
                    newObj.addProperty("InPatientSn",oldObj.has("InPatientSn") ? oldObj.get("InPatientSn").getAsString() : "");
                    param = new Gson().toJson(newObj);
                    logger.info("调取增量服务：" + param);
                    long begin = System.currentTimeMillis();
                    httpClient.executPostWithBody(writeServiceUrl, param, 30000);
                    long end = System.currentTimeMillis();
                    logger.info("etl实时写入ss的时间为： "+ (begin-end));
                }
            } catch (Exception e) {
                logger.info("/gennlife/temp 接口访问不通");
            }
            return httpClient.executPostWithBody(EMPIServerUrl, genParam(inPatientSn,idCard), 30000);
        } catch (Exception e) {
            logger.error("error,{}",e);
        }
        return null;
    }

//    public void write2SS(String inPatientSn, String idCard){
//        if (jedisClusterDao.isExists(inPatientSn) && jedisClusterDao.getValue(inPatientSn).equals("1")){
//            logger.info(inPatientSn + " 该key已经存在");
//            return;
//        } else {
//            try {
//                //方法加入到线程池中去执行
//                executor.execute(new WriteSSThread(inPatientSn, idCard));
//                //1是key存在，0是key不存在
//                jedisClusterDao.putValue(inPatientSn, "1");
//                logger.info(inPatientSn + " 该key写入成功");
//            } catch (Exception e) {
//                logger.info("/gennlife/temp?InPatientSn=&IDCard= 接口不通");
//            }
//        }
//    }

    /**
     * 组织请求empi server 用到的参数
     * @param inpatient_sn
     * @param patient_id
     * @return
     */
    private String genParam(String inpatient_sn, String patient_id){
        JsonObject param = new JsonObject();
        param.addProperty("UserId","fs");
        JsonObject query = new JsonObject();
        if(StringUtils.isNotEmpty(inpatient_sn)){
            byte[] bytes = Base64.decodeBase64(inpatient_sn);
            inpatient_sn = new String(bytes);
            query.addProperty("InPatientSn",inpatient_sn);
        }
        if(StringUtils.isNotEmpty(patient_id)){
            byte[] bytes = Base64.decodeBase64(patient_id);
            patient_id = new String(bytes);
            query.addProperty("IDCard",patient_id);
        }
        param.add("Query",query);
        String params = param.getAsJsonObject().toString();
        logger.info("根据住院号和身份证号组装的参数={}", params);
        return params;
    }

    @Override
    public String patientSN(String empiInfo,String propertyName) {
        JsonObject result = new JsonObject();
        if(StringUtils.isEmpty(empiInfo)){
            result.addProperty("success",false);
            result.addProperty("RESPONSE_ERROR","can not search patient");
            return result.getAsJsonObject().toString();
        }
        JsonObject info = JsonAttrUtil.toJsonElement(empiInfo).getAsJsonObject();
        JsonArray uuids = JsonAttrUtil.getJsonArrayValue(propertyName, info);
        int size = uuids == null ? 0 : uuids.size();

        Gson gson = new Gson();
        if(size >= 0){
            result.addProperty("success",true);
            result.add("pat_sn",uuids);
            return result.getAsJsonObject().toString();
        }else{
            result.addProperty("success",false);
            result.addProperty("RESPONSE_ERROR","can not search  pat_sn");
            return result.getAsJsonObject().toString();
        }
    }


//    private class WriteSSThread implements Runnable {
//        private String inPatientSn;
//        private String idCard;
//
//        public WriteSSThread(String inPatientSn, String idCard){
//            this.inPatientSn = inPatientSn;
//            this.idCard = idCard;
//        }
//
//        @Override
//        public void run() {
//            try {
//                logger.info("开始调用/gennlife/temp ~~~");
//                long begin = System.currentTimeMillis();
//                try {
//                    Thread.sleep(8000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                httpClient.executPostWithBody(writeServiceUrl, new EMPIServiceImpl().genParam(inPatientSn,idCard));
//                long end = System.currentTimeMillis();
//                logger.info("==etl实时写入ss的时间为： "+ (begin-end));
//            } catch (Exception e) {
//                logger.info("/gennlife/temp 接口访问不通");
//            }
//        }
//    }
}
