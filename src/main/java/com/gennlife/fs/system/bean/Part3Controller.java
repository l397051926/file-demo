/**
 * copyRight
 */
package com.gennlife.fs.system.bean;

import com.alibaba.fastjson.JSONObject;
import com.gennlife.fs.common.utils.ParamUtils;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.Part3Service.Part3SampleService;
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
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/part3")
public class Part3Controller {
    private Logger logger = LoggerFactory.getLogger(com.gennlife.fs.controller.Part3Controller.class);
    @Autowired
    private Part3SampleService part3SampleService;

    /**
     * {
     * 	"condition": [
     * 		"([患者基本信息.患者编号] 包含 pat_e9220166dbd261cd02686521203669a4)"
     * 	],
     * 	"page": 1,
     * 	"size": 10,
     * 	"source": "patientDetail",
     * 	"patSns": [
     * 		"",
     * 		"",
     * 		""
     * 	],
     * 	"_scroll_id": "",
     * 	"part3Key": "LANGJIA",
     * 	"uid":""
     * }
     * @param param
     * @return
     */
    @RequestMapping(value = "/applyOutGoing", method = RequestMethod.POST)
    @ResponseBody
    public synchronized String applyOutGoing(@RequestBody String param){
        String resultStr = "";
        try {
            if(StringUtils.isEmpty(param)){
                return wrapperMesage(0,"参数为空");
            }
            JSONObject jsonObject = JSONObject.parseObject(param);
            ConcurrentHashMap<String,JSONObject> infosMap = new ConcurrentHashMap<>();
            //获取弹框内展示的样本数据信息
            resultStr = part3SampleService.applyOutGoing(jsonObject, infosMap);
        } catch (Exception e) {
            logger.error("异常：{}",e.getMessage());
            resultStr = wrapperMesage(0,"异常");
        }
        return resultStr;
    }

    @RequestMapping(value = "/confirmSpecInfos", method = RequestMethod.POST)
    @ResponseBody
    public String confirmSpecInfos(HttpServletRequest requestRe) {
        String param = ParamUtils.getParam(requestRe);
        if (StringUtil.isEmptyStr(param)){
            return wrapperMesage(0, "参数信息错误");
        }
        logger.info("给第三方传输的样本数据信息："+param);
        Long start = System.currentTimeMillis();
        String resultStr = null;
        try {
            resultStr = part3SampleService.confirmSpecInfos(param);
        } catch (Exception e) {
            logger.error("给第三方传输的样本数据信息：", e);
        }
        logger.info("给第三方传输的样本数据信息用时为：" + (System.currentTimeMillis() - start) + "ms");
        return resultStr;
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

    private String wrapperMesage(int code, String message) {
        JSONObject object = new JSONObject();
        object.put("code",code);
        object.put("message",message);
        return object.toJSONString();
    }
}
