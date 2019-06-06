package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.response.SortResponse;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.TimerShaftSort;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.JsonObject;

import static com.gennlife.fs.configurations.model.Model.emrModel;

/**
 * Created by Chenjinfeng on 2018/1/16.
 */
public class Remedy {
    public String getData(String param) {
        String[] s = new String[]{
                "forsz_study",//放疗检查
                "forsz_visit"//放疗疗程
        };
        VisitSNResponse vt = new VisitSNResponse(s, s);
        ResponseInterface sort = new SortResponse(vt, QueryResult.subMap(s), true);
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            JsonObject paramJson =  JsonAttrUtil.toJsonObject(param);
            if(paramJson==null)return ResponseMsgFactory.buildFailStr("参数不是json");
            sort.execute(JsonAttrUtil.toJsonObject(paramJson));
            JsonObject obj = sort.get_result();
            if(obj == null){
                return ResponseMsgFactory.buildFailStr("no data");
            }
            JsonObject data = TimerShaftSort.getInstance().disposeOperator(obj);
            return ResponseMsgFactory.buildResponseStr(data,sort.get_error());
        }else {
            return ResponseMsgFactory.getResponseStr(sort, param);
        }
    }

    public String getRemedy(String param) {
        String[] s = new String[]{
            "forsz_study",//放疗检查
            "forsz_visit"//放疗疗程
        };
        VisitSNResponse vt = new VisitSNResponse(s, s);
        ResponseInterface sort = new SortResponse(vt, QueryResult.subMap(s), true);
        JsonObject paramJson =  JsonAttrUtil.toJsonObject(param);
        if(paramJson==null)return ResponseMsgFactory.buildFailStr("参数不是json");
        sort.execute(JsonAttrUtil.toJsonObject(paramJson));
        JsonObject obj = sort.get_result();
        if(obj == null){
            return ResponseMsgFactory.buildFailStr("no data");
        }
        JsonObject data = TimerShaftSort.getInstance().disposeOperator(obj);
        return ResponseMsgFactory.buildResponseStr(data,sort.get_error());
    }
}
