package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.DateUtil;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.service.patientsdetail.base.PatientDetailService;
import com.gennlife.fs.service.patientsdetail.model.GeneCompator;
import com.gennlife.fs.service.patientsdetail.model.Visit;
import com.gennlife.fs.service.patientsdetail.model.VisitClassifyFollowUp;
import com.gennlife.fs.service.patientsdetail.model.VisitComparatorASC;
import com.gennlife.fs.system.config.DiseasesKeysConfig;
import com.gennlife.fs.system.config.EventStatusUtil;
import com.gennlife.fs.system.config.SectionDiseaseMapConfig;
import com.gennlife.fs.system.config.VisitClassfyConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Chenjinfeng on 2016/10/2.
 */
public class VisitClassify extends PatientDetailService {
    private static Logger logger = LoggerFactory.getLogger(VisitClassify.class);
    private final String START_KEY = "diagnose";
    private Date group_out_date = null;
    private Date group_in_date = null;
    private HashMap<String, Long> countMap = null;
    private HashMap<String, LinkedList<JsonElement>> dataItemMap = null;
    private HashMap<String, TagItem> tagItemMap = new HashMap<String, TagItem>();
    //待确认 第n-1次系统治疗和第n次系统治疗目前不合并
    private static final String[] MERGETAGS = new String[]{"确诊前就诊", "确诊", "系统治疗", "放疗", "复查"};
    //扩展 以结束时间作为条件，进行分页

    private LinkedList otherVisitList = new LinkedList();//伴随
    //扩展 以结束时间作为id，进行分页
    private LinkedList careVisitList = new LinkedList();//关注
    private QueryParam qp;
    // private LinkedList<JsonObject> followsList=null;
    //关注疾病分页
    private int page_size = Integer.MAX_VALUE;
    private int current_page = 1;
    private int countsize = 0;
    private static final String FOLLOW_UP_KEY = "SURGERY_FOLLOW_UP";
    private String section_name = "";
    private String sectionkey = "";
    private VisitClassifyFollowUp followUp = null;
    private boolean isDefault=false;
    public JsonObject getVisitsClassfyInfo(String patient_sn) {
        qp.query_patient_sn(patient_sn);
        qp.addsource("genomics.detection_result");
        qp.addsource(new String[]{
                "patient_info",
                "visits.diagnose", "visits.imaging_exam_diagnosis_reports",
                "visits.pathology_reports", "visits.cell_pathology_reports",
                "visits.operation.OPERATION_INFO.APPROACH",
                "visits.operation.OPERATION_PROCESS.TUMOR_SIZE",
                "visits.operation.OPERATION_PROCESS.TUMOR_LOCATION",
                "visits.operation_records", "visits.chemotherapy",
                "visits.DC_orders", "visits.radiotherapy_orders",
                "visits.radiotherapy", "visits.CT", "visits.MRI", "visits.IVP",
                "visits.AE_combination_drug",//不良事件
                //就诊
                "visits.hospitalization.HOSPITALIZATION_INFO.INPATIENT_COMPLAINT",
                "visits.hospitalization.HOSPITALIZATION_INFO.COMPLAINT_DAYS",
                "visits.hospitalization.PATHOLOGY_DIAGNOSE",
                "visits.hospitalization.DIAGNOSE_TMN",
                "visits.outpatient.PATHOLOGY_DIAGNOSE",
                "visits.outpatient.DIAGNOSE_TMN",
                "Follow_up", "targeted_drug_follow_up",
                "visits.visit_info.ADMISSION_DATE", "visits.visit_info.REGISTERED_DATE",
                "visits.visit_info.EVENT_TYPE","visits.visit_info.VISIT_SN"
        });
        JsonArray sources = VisitClassfyConfig.getData(section_name);
        if (sources == null) return null;
        qp.addsource(sources);
        QueryResult qr = HttpRequestUtils.search(qp);
        JsonObject data = qr.getDataDetailCoverMerge();
        sectionkey = qr.getMergeKey();
        if (sectionkey.equals("DEFAULT"))
            if(!isDefault)section_name = "KIDNEY_CANCER";
            else section_name="DEFAULT";
        else {
            String key = DiseasesKeysConfig.getSection(sectionkey);
            if (StringUtil.isEmptyStr(key))
                logger.error("section key error " + sectionkey);
            else
                section_name = key;
        }
        return data;
    }

    private void init_countMap() {
        countMap = new HashMap<String, Long>();
        for (String key : Visit.getVisits_map_keys())
            countMap.put(key, 0L);
        dataItemMap = new HashMap<String, LinkedList<JsonElement>>();
    }

    public JsonObject getVisitsClassfyInfo(JsonObject param) {
        try {
            String patient_sn = param.get("patient_sn").getAsString();
            if (param.has("section_name"))
                section_name = param.get("section_name").getAsString();
            if(StringUtil.isEmptyStr(section_name)&& SectionDiseaseMapConfig.checkSectionName("DEFAULT"))
            {
                isDefault=true;
                section_name="DEFAULT";
            }
            else if(!SectionDiseaseMapConfig.checkSectionName(section_name))
            {
                if(SectionDiseaseMapConfig.checkSectionName("DEFAULT"))
                {
                    isDefault=true;
                    section_name="DEFAULT";
                }
                else
                {
                    return ResponseMsgFactory.buildFailJson("secion_name is error");
                }
            }
            if (param.has("page_size")) {
                page_size = param.get("page_size").getAsInt();
                if (page_size <= 0) page_size = Integer.MAX_VALUE;
            }
            if (param.has("current_page")) {
                current_page = param.get("current_page").getAsInt();
                if (current_page <= 0) current_page = 1;
            }

            qp = new QueryParam(param);
            JsonObject datas = getVisitsClassfyInfo(patient_sn);
            if (datas == null)
                return ResponseMsgFactory.buildFailJson("no data with " + patient_sn);
            return getVisitsClassfyInfo(patient_sn, section_name, datas);
        } catch (Exception e) {
            logger.error("error ", e);
            return ResponseMsgFactory.buildSystemErrorJson("getinfo ");
        }
    }

    public JsonObject getVisitsClassfyInfo(String patient_sn, String section_name, JsonObject datas) {
        /*if (!SectionDiseaseMapConfig.checkSectionName(section_name.toUpperCase())) {
            logger.warn(" no section name:"+section_name);
            return ResponseMsgFactory.buildFailJson("section_name错误 : "+section_name+" 不存在");

        }*/
        if (datas == null || !datas.has("visits")) {
            return ResponseMsgFactory.buildFailJson("no data or no visits with " + patient_sn);
        }

        JsonObject patientJson = getPatientMedicalHistory(patient_sn, datas);
        JsonArray visits = datas.getAsJsonArray("visits");
        JsonObject adds = getAdds(datas);
        datas = null;

        try {
            divide(visits, adds, section_name);
            visits=null;
            careListCombine(adds);
            //otherListCombine();
            return merge(patientJson);

        } catch (Exception e) {
            logger.error("error ", e);
            return ResponseMsgFactory.buildSystemErrorJson("merge " + patient_sn);
        }


    }

    //提取附加信息
    private JsonObject getAdds(JsonObject datas) {
        JsonObject adds = null;
        String surgery_date = JsonAttrUtil.getStringValue("First_surgery.SURGERY_DATE", datas);
        if (StringUtil.isEmptyStr(surgery_date)) {
            adds = null;
        } else {
            //分子检测需要的附加信息
            JsonArray genoarray = null;
            genoarray = JsonAttrUtil.getJsonArrayValueMutilSource(
                    new String[]{"genomics.detection_result"}, datas);
            if (genoarray != null) {
                if (adds == null) adds = new JsonObject();
                TreeSet<JsonObject> detection_result = new TreeSet<>(new GeneCompator());
                for (JsonElement elem : genoarray)
                    genomics_filter(null, detection_result, elem);

                if (detection_result != null) {
                    adds.addProperty("surgery_date", surgery_date);
                    adds.add("genomics", JsonAttrUtil.toJsonTree(detection_result));
                }
            }
        }

        //随访
        followUp = new VisitClassifyFollowUp(datas);
       /* //靶向治疗
        if(datas.has("targeted_drug"))
        {
            if(adds == null) adds=new JsonObject();
            adds.add("targeted_drug",datas.get("targeted_drug"));

        }*/
        return adds;
    }


    //个人病史
    public JsonObject getPatientMedicalHistory(String patient_sn, JsonObject personinfo) {

        JsonObject medicalhistory = new JsonObject();
        medicalhistory.addProperty("patient_sn", patient_sn);
        //患者基本信息
        medicalhistory.add("patient_info",
                JsonAttrUtil.getOneFromJsonArray(new String[]{"GENDER", "BIRTH_DATE"}, personinfo.get("patient_info")));
        //个人肿瘤史
        JsonAttrUtil.safelyAdd(medicalhistory, "cancer_history", personinfo.get("cancer_history"));
        //既往疾病史
        JsonAttrUtil.safelyAdd(medicalhistory, "disease_history", personinfo.get("disease_history"));
        return medicalhistory;
    }

    //数据划分 分为关注的与其他
    public void divide(JsonArray visits, JsonObject adds, String section_name) throws Exception {
        String codes = "";
        for (JsonElement visit : visits) {
            JsonObject visitItem = visit.getAsJsonObject();
            Visit tmp = new Visit(visitItem);
            if (visitItem.has("diagnose")) {
                tmp.init_normal_code(visitItem.getAsJsonArray("diagnose"));
            }
            if (!SectionDiseaseMapConfig.isBelong(tmp.get_disease_normal_code(), section_name.toUpperCase())) {
                //先不弄other
                // if (tmp.init_disease_Type()) otherVisitList.add(tmp);
            } else {
                tmp.saveClassicfyData(visitItem, adds);
                if (!tmp.ismapEmpty()) careVisitList.add(tmp);
            }

        }
        //logger.info("careVisitList size "+careVisitList.size());
        //logger.info("otherVisitList size "+otherVisitList.size());
        //clear
        visits = null;
    }

    //联合数据，得到最后结果
    private JsonObject merge(JsonObject data) {

        List<JsonElement> caretmp = null;
        data.add("count", JsonAttrUtil.toJsonTree(countMap));
        countsize = careVisitList.size();
        data.addProperty("allpage", Math.ceil(countsize * 1.0 / page_size));
        caretmp = JsonAttrUtil.getPagingResult(careVisitList, page_size, current_page);
        //data.add("follow", JsonAttrUtil.toJsonTree(otherVisitList));
        data.add("care", JsonAttrUtil.toJsonTree(caretmp));
        data.addProperty("page_size", page_size);
        data.addProperty("current_page", current_page);
        data.addProperty("key", sectionkey);
        data.addProperty("section_name", section_name);
        return data;
    }

    private void careListCombine(JsonObject adds) {
        init_countMap();

        Collections.sort(careVisitList, new VisitComparatorASC());
        LinkedList<JsonElement> groupdataList = new LinkedList<JsonElement>();//组内集合
        Iterator iter = careVisitList.iterator();

        int id = 1;
        LinkedList<JsonElement> groupList = new LinkedList<JsonElement>();//大组集合
        do if (iter.hasNext()) {

            Visit pre = (Visit) iter.next();//从第一个元素开始扫描
            Visit after = pre;
            boolean conn = true;
            //取不同分类的值，合并可以合并的
            while (iter.hasNext()) {
                after = (Visit) iter.next();
                //事件合并
                conn = ismerge(pre, after);
                if (conn) {
                    addAllType(pre);
                    pre = after;
                } else {//不同事件，退出
                    addAllType(pre);
                    iter = careVisitList.iterator();//指针归位
                    break;
                }
            }

            //最后一项，直接加入
            if (conn) {
                addAllType(pre);
            }

            //一轮结束
            for (String type_key : Visit.getVisits_map_keys()) {
                //临时处理evaluation
                if (type_key.equals("evaluation")) {
                    //tagItemMap.evaluation
                    LinkedList<JsonElement> tmplist = get_list_from_map(type_key);
                    for (String key : EventStatusUtil.get_evaluation()) {
                        if (tagItemMap.containsKey(key)) {
                            JsonObject evalJson = new JsonObject();
                            evalJson.addProperty("NAME", key);
                            tmplist.add(evalJson);
                        }
                    }
                    if (tmplist.size() > 0) {
                        JsonObject evaluation = new JsonObject();
                        evaluation.add("evaluation", JsonAttrUtil.toJsonTree(tmplist));
                        tmplist.clear();
                        tmplist.add(evaluation);
                        dataItemMap.put(type_key, tmplist);
                    }
                }
                //临时处理结束

                if (!dataItemMap.containsKey(type_key)) continue;

                LinkedList<JsonElement> dataItem = dataItemMap.get(type_key);
                if (dataItem.size() > 0) {
                    JsonObject groupItem = new JsonObject();
                    groupItem.addProperty("type", type_key);

                    long itemsize = countSize(type_key, dataItem);
                    groupItem.addProperty("size", itemsize);
                    dataItem = clearEmpty(dataItem);
                    groupItem.add("content", JsonAttrUtil.toJsonTree(dataItem));
                    //计算各类卡片总个数
                    Long sum = countMap.get(type_key) + itemsize;
                    countMap.put(type_key, sum);
                    groupdataList.add(groupItem);

                }
            }

            dataItemMap.clear();
            //元素扫描完成,一个组添加完毕

            //去空visit
            Visit tmp = null;
            int i = 0;
            while (i < careVisitList.size()) {
                tmp = (Visit) careVisitList.get(i);
                if (tmp.ismapEmpty())
                    careVisitList.remove(i);
                else
                    i++;
            }

            if (groupdataList.size() > 0) {
                JsonObject group = new JsonObject();
                group.addProperty("id", id);
                if (group_in_date != null) group.addProperty("start_date", DateUtil.getDateStr_ymd(group_in_date));
                if (group_out_date != null) group.addProperty("end_date", DateUtil.getDateStr_ymd(group_out_date));
                //tag处理
                tagTreate();
                LinkedList<TagItem> tmplist = new LinkedList<>();
                for (String key : tagItemMap.keySet()) {

                    tmplist.add(tagItemMap.get(key));
                }
                Collections.sort(tmplist);

                //附加处理


                //
                group.add("lines", JsonAttrUtil.toJsonTree(tmplist));
                group.add("group", JsonAttrUtil.toJsonTree(groupdataList));
                groupList.add(group);
                id++;
            }
            //复位
            groupdataList.clear();
            tagItemMap.clear();
            group_out_date = null;
            group_in_date = null;

            iter = careVisitList.iterator(); //指针归位

        } while (iter.hasNext());
        //logger.info("followUP " + followUp.isEmpty());
        Date last = null;
        if (!followUp.isEmpty()) {
            Iterator<JsonElement> groupiter = groupList.iterator();
            JsonObject preJson = null;
            JsonObject nextJson = null;
            while (groupiter.hasNext()) {
                preJson = nextJson;
                nextJson = groupiter.next().getAsJsonObject();
                if (preJson == null) {
                    preJson = nextJson;
                    if (groupiter.hasNext()) nextJson = groupiter.next().getAsJsonObject();
                    else nextJson = null;
                }
                Date st = DateUtil.getDate_ymd(preJson.get("start_date").getAsString());
                Date et = null;
                if (nextJson != null) et = DateUtil.getDate_ymd(nextJson.get("start_date").getAsString());
                if (et == null) last = st;
                else last = et;
                LinkedList<JsonElement> followtmplist = new LinkedList<>();
                JsonObject followjson = null;

                do {
                    followjson = followUp.getFollowUp(st, et);
                    if (followjson != null) {
                        followtmplist.add(followjson);
                    }
                } while (followjson != null);
                if (followtmplist.size() == 0 && groupiter.hasNext()) continue;
                saveFollowUp(preJson, followtmplist);
                /*logger.info(" follows"+JsonAttrUtil.toJsonStr(JsonAttrUtil.toJsonTree(followtmplist)));
                */
                if (groupiter.hasNext() == false) {
                    followtmplist.clear();
                    do {
                        followjson = followUp.getFollowUp(last, null);

                        if (followjson != null) {
                            followtmplist.add(followjson);
                        }
                    } while (followjson != null);
                    if (followtmplist.size() > 0) {
                        if (nextJson != null)
                            preJson = nextJson;
                        saveFollowUp(preJson, followtmplist);
                    }
                }

            }

        }


        careVisitList = groupList;
    }

    private void saveFollowUp(JsonObject preJson, LinkedList<JsonElement> followtmplist) {
        if (followtmplist.size() == 0) return;
        JsonObject followgroupJson = new JsonObject();
        followgroupJson.addProperty("type", "follow_up");
        followgroupJson.add("content", JsonAttrUtil.toJsonTree(followtmplist));
        long size = count(followtmplist, followUp.getHashKeys());
        countMap.put("follow_up", countMap.get("follow_up") + size);
        followgroupJson.addProperty("size", size);
        preJson.getAsJsonArray("group").add(followgroupJson);
    }

    private LinkedList<JsonElement> clearEmpty(LinkedList<JsonElement> dataItem) {
        for (JsonElement elem : dataItem) {
            //非全空
            if (elem.getAsJsonObject().entrySet().size() > 0) return dataItem;
        }
        JsonElement tmp = dataItem.getLast();
        dataItem.clear();
        dataItem.add(tmp);
        return dataItem;
    }

    private Long countSize(String key, LinkedList<JsonElement> dataItem) {

        switch (key) {
            case "diagnose":
                return count(dataItem, "diagnose");
            case "imaging_exam_diagnosis_reports":
                return count(dataItem, "imaging_exam_diagnosis_reports");

            case "pathology_reports":
                return count(dataItem, "cell_pathology_reports",
                        "pathology_reports");

            case "operation":
                return count(dataItem, "operation");
            case "genomics":
                return 1L;
            case "systemic_therapy":
                return count(dataItem, "chemotherapy", "targeted_drug",
                        "DC_orders");
            case "radiotherapy":
                return count(dataItem, "radiotherapy", "radiotherapy_orders");

            case "adverse_event":
                return count(dataItem, "adverse_event");

            case "follow_up":
                return count(dataItem, followUp.getHashKeys());
            case "evaluation":
                return count(dataItem, "evaluation");
        }
        return 0L;
    }

    private long count(LinkedList<JsonElement> dataItem, String... keys) {
        long tmp = 0L;
        int arrsize = 0;
        for (JsonElement item : dataItem) {
            JsonObject json = item.getAsJsonObject();
            //数据丢失
            if (json.entrySet().size() == 0) {
                tmp++;
            }
            for (String key : keys) {
                if (json.has(key) && json.get(key).isJsonArray()) {
                    arrsize = json.getAsJsonArray(key).size();
                    //数据丢失
                    if (arrsize == 0) arrsize = 1;
                    tmp += arrsize;
                }
            }

        }
        return tmp;
    }

    private LinkedList<JsonElement> get_list_from_map(String type_key) {
        LinkedList<JsonElement> tmplist = dataItemMap.get(type_key);
        if (tmplist == null) tmplist = new LinkedList<JsonElement>();
        return tmplist;
    }

    //tag 处理 留次数 去疗效评估
    private void tagTreate() {
        Iterator<String> tagiter = tagItemMap.keySet().iterator();
        while (tagiter.hasNext()) {
            String key = tagiter.next();
            String findkey = StringUtil.getTimesEvent(key);
            if (findkey != null) {
                //时间段类型不要次数
                if (is_duration_tag(findkey)) {
                    if (tagItemMap.containsKey(findkey)) {
                        TagItem delItem = tagItemMap.remove(key);
                        TagItem target = tagItemMap.get(findkey);
                        target.add_duration(delItem);
                    } else {
                        tagItemMap.put(findkey, tagItemMap.remove(key));
                    }
                    tagiter = tagItemMap.keySet().iterator();
                } else {//留次数
                    if (tagItemMap.containsKey(findkey)) {
                        TagItem delItem = tagItemMap.remove(findkey);
                        if (is_duration_tag(findkey)) {
                            TagItem target = tagItemMap.get(key);
                            target.add_duration(delItem);
                        }
                        tagiter = tagItemMap.keySet().iterator();
                    }
                }

            }
            //去疗效评估
            if (StringUtil.isInStrs(key, EventStatusUtil.get_evaluation())) {
                if (tagItemMap.remove("疗效评估") != null)
                    tagiter = tagItemMap.keySet().iterator();
            }
            //


        }
    }

    //时间段事件
    private boolean is_duration_tag(String key) {
        for (int index = 0; index < MERGETAGS.length; index++)
            if (key.contains(MERGETAGS[index]))
                return true;
        return false;
    }

    private void addAllType(Visit pre) {
        LinkedList<JsonElement> dataItem = null;
        if (pre.getMapValue(START_KEY) == null) {
            return;
        }
        for (String type_key : Visit.getVisits_map_keys()) {
            if (pre.getMapValue(type_key) == null) continue;

            if (dataItemMap.containsKey(type_key)) {
                dataItem = dataItemMap.get(type_key);
            }
            if (dataItem == null)
                dataItem = new LinkedList<JsonElement>();

            addItemToList(dataItem, pre, type_key);
            if (dataItem.size() > 0) {
                dataItemMap.put(type_key, dataItem);
            }
            dataItem = null;
        }
    }

    private void addItemToList(LinkedList<JsonElement> dataItem, Visit pre, String key) {
        JsonElement value = pre.removeMapValue(key);
        if (value != null) {
            if (key.equals(START_KEY)) {
                //tag初始化
                tagMapUpdate(pre);
                //大组 时间 更新
                update_group_time(pre);
            }
            dataItem.add(value);

        }
    }

    private void tagMapUpdate(Visit pre) {
        for (String eventItem : pre.get_event_type()) {

            if (is_duration_tag(eventItem)) {
                TagItem tagItemtmp = null;
                if (tagItemMap.containsKey(eventItem))
                    tagItemtmp = tagItemMap.get(eventItem);
                else
                    tagItemtmp = new TagItem(pre.get_visit_date(), eventItem);

                tagItemtmp.count(pre.getVisit_item_in_date(), pre.getVisit_item_out_date());
                tagItemMap.put(eventItem, tagItemtmp);

            } else {
                //时间点类型
                if (!tagItemMap.containsKey(eventItem))
                    tagItemMap.put(eventItem, new TagItem(pre.get_visit_date(), eventItem));
            }
        }
    }

    private void update_group_time(Visit pre) {
        if (group_in_date == null || group_in_date.compareTo(pre.getVisit_item_in_date()) > 0)
            group_in_date = pre.getVisit_item_in_date();

        if (group_out_date == null) {
            if (pre.getVisit_item_out_date() == null)
                group_out_date = pre.getVisit_item_in_date();
            else
                group_out_date = pre.getVisit_item_out_date();

        } else {
            if (pre.getVisit_item_out_date() == null) {
                if (group_out_date.compareTo(pre.getVisit_item_in_date()) < 0)
                    group_out_date = pre.getVisit_item_in_date();
            } else {
                if (group_out_date.compareTo(pre.getVisit_item_out_date()) < 0)
                    group_out_date = pre.getVisit_item_out_date();
            }
        }
    }


    //伴随疾病
    private void otherListCombine() {
        //时间升序
        Collections.sort(otherVisitList, new VisitComparatorASC());
        LinkedList<JsonElement> list = new LinkedList<JsonElement>();
        for (Object elem : otherVisitList)
            list.add(((Visit) elem).get_classify_other_detail());
        otherVisitList = list;

    }

    private String gettag(List<String> event) {
        String result = "";
        if (event == null || event.size() == 0) return result;
        for (String item : MERGETAGS)
            if (event.contains(item))
                result = result + " " + item;
        return result;

    }

    //是否合并
    private boolean ismerge(Visit pre, Visit after) {
        String pretag = gettag(pre.get_event_type());
        String aftertag = gettag(after.get_event_type());
        if ("".equals(pretag) || "".equals(aftertag))
            return false;
        return aftertag.equals(pretag);
    }

    class TagItem implements Comparable<TagItem> {
        private String startdate;
        private Long duration;
        private String key;
        transient private Date _start_date,_end_date;
        TagItem(String startdate, String key) {
            this.startdate = startdate.split(" ")[0];
            duration = 0L;
            this.key = key;
        }

        public long get_duration() {
            return duration;
        }

        public void add_duration(TagItem item) {
            this.duration += item.get_duration();
        }

        public String getStartdate() {
            return startdate;
        }

        protected void count(Date start_date, Date end_date) {
            if (start_date == null) return;
            if (_start_date == null || _start_date.compareTo(start_date) > 0) _start_date = start_date;
            if (end_date == null) end_date = start_date;
            if (_end_date == null || _end_date.compareTo(end_date) < 0) _end_date = end_date;
            if (_start_date != null && _end_date != null) {
                duration = DateUtil.getDurationWithDays(_start_date, _end_date);
            } else {
                duration = 1L;
            }
            if (duration <= 0) duration = 1L;

        }


        @Override
        public int compareTo(TagItem o) {
            return startdate.compareTo(o.getStartdate());

        }
    }
}