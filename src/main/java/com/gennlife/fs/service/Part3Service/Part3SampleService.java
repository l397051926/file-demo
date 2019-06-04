package com.gennlife.fs.service.Part3Service;

import com.alibaba.fastjson.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wangyiyan on 2019/4/8
 */
public interface Part3SampleService {
    //提供可选择的样本数据信息
    String applyOutGoing(JSONObject jsonObject, ConcurrentHashMap<String, JSONObject> infosMap);
    //密码解密
    String encryptPWD(String param);
    //确认需要导出的样本数据
    String confirmSpecInfos(String param);
}
