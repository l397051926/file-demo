package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.comparator.JsonComparatorDESCByKey;
import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.response.PaginationMemoryResponse;
import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.response.SortResponse;
import com.gennlife.fs.common.utils.DateUtil;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.PagingUtils;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

import static com.gennlife.fs.common.utils.ApplicationContextHelper.getBean;
import static com.gennlife.fs.configurations.model.Model.emrModel;

public class PharmarcyOrder {

    public String getPharmarcyOrder(String param) {
        String drug_order = "medicine_order";
        String DRUG_GENERIC_NAME = "MEDICINE_NAME";
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            drug_order = "drug_order";
            DRUG_GENERIC_NAME = "DRUG_GENERIC_NAME";
        }
        ResponseInterface vt=new PaginationMemoryResponse(new SortResponse(new VisitSNResponse(drug_order,"orders"),"orders", QueryResult.getSortKey(drug_order),true),"orders");
        return ResponseMsgFactory.getResponseStr(vt,param);
    }

    public String getNewPharmarcyOrder(String param) {
        String drug_order = "medicine_order";
        String DRUG_GENERIC_NAME = "MEDICINE_NAME";
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            drug_order = "drug_order";
            DRUG_GENERIC_NAME = "DRUG_GENERIC_NAME";
        }
        VisitSNResponse vt =  new VisitSNResponse(drug_order,
            drug_order);
        JsonObject paramJson = JsonAttrUtil.toJsonObject(param);
        if(paramJson==null)return ResponseMsgFactory.buildFailStr("参数不是json");

        JsonArray orderType = paramJson.get("orderType").getAsJsonArray();
        String orderStatus = paramJson.get("orderStatus").getAsString();
        String medicineName = paramJson.get("medicineName").getAsString();

        Set<String> orderStatusArray  = new HashSet<>();

        vt.execute(JsonAttrUtil.toJsonObject(paramJson));
        JsonObject result = vt.get_result();
        if(result == null ){
            return ResponseMsgFactory.buildFailStr("no data");
        }
        JsonArray medicin = result.get("medicine_order").getAsJsonArray();

        boolean isOrderType = true;
        boolean isOrderStatus = true;
        boolean isMedicinaName = true;
        List<JsonElement> medicineNullList = new LinkedList<>();
        List<MedicineSortClass> sortList = new LinkedList<>();
        for (JsonElement element : medicin){
            JsonObject object = element.getAsJsonObject();
            String order_status = JsonAttrUtil.getStringValue("ORDER_STATUS_NAME",object);
            if(StringUtil.isNotEmptyStr(order_status)){
                orderStatusArray.add(order_status);
            }
            if(orderType.size()>0){
                String long_once_flag = JsonAttrUtil.getStringValue("LONG_ONCE_FLAG",object);
                isOrderType = orderType.contains(JsonAttrUtil.toJsonElement(long_once_flag));
            }else {
                isOrderStatus = false;
            }

            if(StringUtil.isNotEmptyStr(orderStatus)){
                isOrderStatus = orderStatus.equals(order_status);
            }

            if(StringUtil.isNotEmptyStr(medicineName)){
                String medicine_name = JsonAttrUtil.getStringValue(DRUG_GENERIC_NAME,object);
                isMedicinaName = StringUtil.isNotEmptyStr(medicine_name) && medicine_name.contains(medicineName);
            }

            if(isOrderType && isOrderStatus && isMedicinaName){
                String parent_order_sn = JsonAttrUtil.getStringValue("PARENT_ORDER_SN",object);
                String order_start_time = JsonAttrUtil.getStringValue("ORDER_START_TIME",object);
                if (StringUtil.isNotEmptyStr(parent_order_sn)){
                    MedicineSortClass m = new MedicineSortClass(parent_order_sn,order_start_time);
                    if(sortList.contains(m)){
                        MedicineSortClass mtmp = sortList.get(sortList.indexOf(m));
                        if(StringUtil.isEmptyStr(mtmp.getTime())){
                            mtmp.setTime(order_start_time);
                        }
                        if(StringUtil.isNotEmptyStr(order_start_time) && mtmp.getTime().compareTo(m.getTime()) < 0 ){
                            mtmp.setTime(order_start_time);
                        }
                        mtmp.addDtes(object);
                    }else {
                        List<JsonElement> le = new LinkedList<>();
                        le.add(object);
                        m.setDates(le);
                        sortList.add(m);
                    }
                }else {
                    medicineNullList.add(object);
                }
            }
        }
        Collections.sort(sortList, (o1, o2) -> o2.getTime().compareTo(o1.getTime()));
        JsonArray resultArray = new JsonArray();
        for (MedicineSortClass m : sortList){
            List<JsonElement> tmpList = m.getDates();
            tmpList = JsonAttrUtil.sort(tmpList, new JsonComparatorDESCByKey("ORDER_START_TIME"));
            for (JsonElement element : tmpList){
                resultArray.add(element);
            }
        }

        medicineNullList =  JsonAttrUtil.sort(medicineNullList, new JsonComparatorDESCByKey("ORDER_START_TIME"));
        resultArray.addAll(JsonAttrUtil.toJsonTree(medicineNullList).getAsJsonArray());
        result.add("medicine_order",resultArray);
        result.add("orderStatus",JsonAttrUtil.toJsonTree(orderStatusArray));
        return ResponseMsgFactory.buildResponseStr(result,vt.get_error());
    }

    public String getNewOrdersPharmacy(String param) {
        String non_drug_orders = "orders";
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            non_drug_orders = "non_drug_orders";
        }
        ResponseInterface vt=new PaginationMemoryResponse(new SortResponse(new VisitSNResponse(non_drug_orders,non_drug_orders),non_drug_orders, QueryResult.getSortKey(non_drug_orders),false),non_drug_orders);
        JsonObject paramJson = JsonAttrUtil.toJsonObject(param);
        if(paramJson==null)return ResponseMsgFactory.buildFailStr("参数不是json");

        JsonArray orderType = paramJson.get("orderType").getAsJsonArray();
        String medicineName = paramJson.get("medicineName").getAsString();
        String orderStatus = paramJson.get("orderStatus").getAsString();
        String willType = paramJson.get("willType").getAsString();

        Set<String> orderStatusArray = new HashSet<>();
        Set<String> willTypeArray = new HashSet<>();

        vt.execute(JsonAttrUtil.toJsonObject( JsonAttrUtil.toJsonObject(param)));
        JsonObject obj = vt.get_result();
        if(obj == null){
            return ResponseMsgFactory.buildFailStr("no data");
        }
        JsonArray res = obj.get("orders").getAsJsonArray();
        JsonArray result = new JsonArray();
        boolean isOrderType = true;
        boolean isMedicinaName = true;
        boolean isOrderStatus = true;
        boolean isWillType = true;

        for (JsonElement element : res){
            JsonObject object = element.getAsJsonObject();
            String order_status_name = JsonAttrUtil.getStringValue("ORDER_STATUS_NAME",object);
            String order_type_name = JsonAttrUtil.getStringValue("ORDER_TYPE_NAME",object);
            if(StringUtil.isNotEmptyStr(order_status_name) ){
                orderStatusArray.add(order_status_name);
            }
            if(StringUtil.isNotEmptyStr(order_type_name)){
                willTypeArray.add(order_type_name);
            }
            if(orderType.size()>0){
                String long_once_flag = JsonAttrUtil.getStringValue("LONG_ONCE_FLAG",object);
                isOrderType = orderType.contains(JsonAttrUtil.toJsonElement(long_once_flag));
            }else {
                isOrderType = false;
            }
            if(StringUtil.isNotEmptyStr(medicineName)){
                String medicine_name = JsonAttrUtil.getStringValue("ORDER_NAME",object);
                isMedicinaName = StringUtil.isNotEmptyStr(medicine_name) && medicine_name.contains(medicineName);
            }
            if(StringUtil.isNotEmptyStr(orderStatus)){
                isOrderStatus = orderStatus.equals(order_status_name);
            }
            if(StringUtil.isNotEmptyStr(willType)){
                isWillType = willType.equals(order_type_name);
            }
            if(isOrderType && isMedicinaName && isWillType && isOrderStatus){
                result.add(object);
            }
        }
        obj.add("orders",result);
        obj.add("orderStatus",JsonAttrUtil.toJsonTree(orderStatusArray));
        obj.add("willType",JsonAttrUtil.toJsonTree(willTypeArray));
        return  ResponseMsgFactory.buildResponseStr(obj,vt.get_error());
    }

    public String getOrdersPharmacyDay(String param) {
        String non_drug_orders = "orders";
        if (emrModel().version().mainVersion().isHigherThanOrEqualTo(4)) {
            non_drug_orders = "non_drug_orders";
        }
        ResponseInterface vt=new PaginationMemoryResponse(
            new SortResponse(
                new VisitSNResponse(non_drug_orders,
                    non_drug_orders),
                non_drug_orders, QueryResult.getSortKey(non_drug_orders),false),non_drug_orders);
        JsonObject paramJson = JsonAttrUtil.toJsonObject(param);
        String time = JsonAttrUtil.getStringValue("time",paramJson);
        Integer page = paramJson.get("page").getAsInt();
        Integer size = paramJson.get("size").getAsInt();
        if(paramJson==null){
            return ResponseMsgFactory.buildFailStr("参数不是json");
        }
        vt.execute(JsonAttrUtil.toJsonObject( JsonAttrUtil.toJsonObject(param)));
        JsonObject obj = vt.get_result();
        if(obj == null){
            return ResponseMsgFactory.buildFailStr("no data");
        }
        JsonArray res = obj.get("orders").getAsJsonArray();
        List<JsonObject> resultList = new LinkedList<>();

        for (JsonElement element : res){
            JsonObject object = element.getAsJsonObject();
            String times = DateUtil.getDateStr_ymd(JsonAttrUtil.getStringValue("ORDER_START_TIME",object));
            if(time.equals(times)){
                String titleName = JsonAttrUtil.getStringValue("ORDER_NAME",object);
                if(StringUtil.isEmptyStr(titleName)){
                    titleName = "非药品医嘱";
                }
                object.addProperty("titleName",titleName);
                object.addProperty("configSchema","orders");
                resultList.add(object);
            }
        }
        List<JsonObject> resultData = PagingUtils.getPageContentByApi(resultList,page,size);
        JsonObject data = new JsonObject();
        data.add("orders",JsonAttrUtil.toJsonTree(resultData));
        JsonObject result = new JsonObject();
        result.addProperty("code",1);
        result.addProperty("msg","success");
        result.add("data",data);
        result.addProperty("total",resultList.size());
        return JsonAttrUtil.toJsonStr(result);

    }

    static class MedicineSortClass{
        private String partentOrderSn ;
        private String time;
        private List<JsonElement> dates;

        public MedicineSortClass(String partentOrderSn, String time) {
            this.partentOrderSn = partentOrderSn;
            this.time = time;
        }
        public void addDtes(JsonElement element){
            this.dates.add(element);
        }

        public void setDates(List<JsonElement> dates) {
            this.dates = dates;
        }

        public List<JsonElement> getDates() {
            return dates;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null ) return false;
            MedicineSortClass that = (MedicineSortClass) o;
            return Objects.equals(partentOrderSn, that.partentOrderSn);
        }

        @Override
        public int hashCode() {

            return Objects.hash(partentOrderSn);
        }
    }


}