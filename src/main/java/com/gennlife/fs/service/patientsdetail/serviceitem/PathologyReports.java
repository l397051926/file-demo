package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.ImageResponseUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.google.gson.JsonObject;

import static com.gennlife.fs.common.utils.ApplicationContextHelper.getBean;
import static com.gennlife.fs.configurations.model.Model.emrModel;


/**
 * Created by zhangshijian on 2016/7/18.
 * 分类详情-检验检查-病理检测
 */
public class PathologyReports {

    public String getPathologyReports(String param) {
        String pathology_report = "pathology_reports";
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            pathology_report = "pathology_report";
        }
        ResponseInterface template = ImageResponseUtils.getImageResponseInterface(pathology_report);
        JsonObject paramJson = JsonAttrUtil.toJsonObject(param);
        if(paramJson==null)return ResponseMsgFactory.buildFailStr("参数不是json");
        template.execute(JsonAttrUtil.toJsonObject(paramJson));
        JsonObject data = template.get_result();
        data.addProperty("configSchema",pathology_report);
        return ResponseMsgFactory.buildResponseStr(data,template.get_error());
    }

}
