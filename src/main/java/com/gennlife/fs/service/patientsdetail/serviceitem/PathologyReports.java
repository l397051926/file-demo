package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.ImageResponseUtils;


/**
 * Created by zhangshijian on 2016/7/18.
 * 分类详情-检验检查-病理检测
 */
public class PathologyReports {
    public String getPathologyReports(String param) {
        ResponseInterface template = ImageResponseUtils.getImageResponseInterface("pathology_reports");
        return ResponseMsgFactory.getResponseStr(template, param);

    }
}
