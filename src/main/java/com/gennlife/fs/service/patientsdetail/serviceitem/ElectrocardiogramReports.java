package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.response.SortResponse;
import com.gennlife.fs.common.utils.ImageResponseUtils;

/**
 * Created by Chenjinfeng on 2018/1/16.
 */
public class ElectrocardiogramReports {
    public String getData(String param) {

        String[] keys = new String[]{
                "electrocardiogram_reports" //心电图报告
        };
        ResponseInterface template = ImageResponseUtils.getImageResponseInterface(keys);
        return ResponseMsgFactory.getResponseStr(new SortResponse(template, "electrocardiogram_reports", QueryResult.getSortKey("electrocardiogram_reports"), true), param);
    }
}
