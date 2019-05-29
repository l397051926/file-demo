package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.ImageResponseUtils;
import com.gennlife.fs.configurations.GeneralConfiguration;

import static com.gennlife.fs.common.utils.ApplicationContextHelper.getBean;


/**
 * Created by zhangshijian on 2016/7/18.
 * 分类详情-检验检查-病理检测
 */
public class PathologyReports {
    private GeneralConfiguration cfg = getBean(GeneralConfiguration.class);
    public String getPathologyReports(String param) {
        String pathology_report = "pathology_reports";
        if (cfg.patientDetailModelVersion.compareTo("4") >= 0) {
            pathology_report = "pathology_report";
        }
        ResponseInterface template = ImageResponseUtils.getImageResponseInterface(pathology_report);
        return ResponseMsgFactory.getResponseStr(template, param);

    }
}
