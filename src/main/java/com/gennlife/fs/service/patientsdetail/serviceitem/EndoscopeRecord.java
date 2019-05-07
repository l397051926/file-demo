package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.ImageResponseUtils;

/**
 * Created by Helios on 2016/9/14.
 * 分类详情-检验检查-镜检
 */
public class EndoscopeRecord {
    public String getEndoscopeRecord(String param) {
        ResponseInterface template = ImageResponseUtils.getImageResponseInterface("microscopic_exam_reports");
        return ResponseMsgFactory.getResponseStr(template, param);
    }
}