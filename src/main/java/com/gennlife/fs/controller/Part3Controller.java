/**
 * copyRight
 */
package com.gennlife.fs.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.fs.common.utils.ParamUtils;
import com.gennlife.fs.service.Part3Service.Part3SampleService;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/part3/")
public class Part3Controller {
    private Logger logger = LoggerFactory.getLogger(Part3Controller.class);
    @Autowired
    private Part3SampleService part3SampleService;

    /**
     * {
     *     "condition":["([患者基本信息.性别] 包含 男) AND {[就诊.就诊基本信息.就诊次数] > 10}"],
     *     "part3Key":"LANGJIA"
     * }
     * @param param
     * @return
     */
    @RequestMapping(value = "/applyOutGoing", method = RequestMethod.POST)
    @ResponseBody
    public synchronized String applyOutGoing(@RequestBody String param){
        logger.info("参数："+param);
        try {
            if(StringUtils.isEmpty(param)){
                return wrapperMesage("300","参数为空");
            }
            JSONArray sampleNumber = new JSONArray();
            JSONObject jsonObject = JSONObject.parseObject(param);
            ConcurrentHashMap<String,JSONObject> results = new ConcurrentHashMap<>();
            JSONArray condition = jsonObject.getJSONArray("condition");

            /*获取所有样本号*/
            int size = condition == null ? 0 : condition.size();
            for (int i = 0; i<size; i++) {
                String param1 = condition.getString(i);
                part3SampleService.getSampleNumber(param1, results);
            }

            Collection<JSONObject> values = results.values();
            sampleNumber.addAll(values);
            if (sampleNumber.size() == 0){
                JsonObject resultObj = new JsonObject();
                resultObj.addProperty("info","列表中患者均无标本信息");
                return resultObj.toString();
            }

            /*获取token*/
            JsonObject ouath = part3SampleService.ouath();
            if(!StringUtils.equalsIgnoreCase(ouath.get("status").getAsString(),"200")){
                return ouath.toString();
            }
            String access_token = ouath.get("access_token")==null? null:ouath.get("access_token").getAsString();

            /*获取出库信息*/
            logger.info("获取出库信息，标本号为： " + sampleNumber.toJSONString());
            String result = part3SampleService.applyOutGoing(access_token, sampleNumber.toJSONString());
            logger.info("返回信息："+result);
            return result;
        } catch (Exception e) {
            String message = e.getMessage();
            logger.error("异常：{}",e);
            return wrapperMesage("500",message);
        }
    }


    private String wrapperMesage(String status, String message) {
        JSONObject object = new JSONObject();
        object.put("status",status);
        object.put("message",message);
        return object.toJSONString();
    }

    @RequestMapping(value = "/encryptPWD", method = RequestMethod.POST)
    @ResponseBody
    public String encryptPWD(HttpServletRequest requestRe) {
        String param = ParamUtils.getParam(requestRe);
        logger.info("密码解密："+param);
        Long start = System.currentTimeMillis();
        String resultStr = null;
        try {
            resultStr = part3SampleService.encryptPWD(param);
        } catch (Exception e) {
            logger.error("RSA解密密码异常：", e);
        }
        logger.info("RSA解密用时为：" + (System.currentTimeMillis() - start) + "ms");
        return resultStr;
    }
}
