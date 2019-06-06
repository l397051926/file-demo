package com.gennlife.fs.controller;

import com.gennlife.fs.common.cache.MergeRequestInLocal;
import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.service.empi.EMPIService;
import com.gennlife.fs.service.patientsdetail.model.EachVisitResponse;
import com.gennlife.fs.service.patientsdetail.serviceitem.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * 分类详情浏览Controller
 */
@Controller
@RequestMapping(value = "/PatientDetail", produces = "application/json;charset=UTF-8")
public class PatientDetailController {
    private Logger logger = LoggerFactory.getLogger(PatientDetailController.class);
    private ChoiceList choiceList = new ChoiceList();

    @Autowired
    private EMPIService empiService;

    //门诊病历
    @RequestMapping(value = "/clinic_medical_records", method = RequestMethod.POST)
    public
    @ResponseBody
    String getClinicMedicalRecords(@RequestBody String param) {
        return new ClinicMedicalRecords().getData(param);
    }

    /*
     * 标题栏、患者基本信息
     */
    @RequestMapping(value = "/patient_basic_info", method = RequestMethod.POST)
    public
    @ResponseBody
    String getPatientInfo(@RequestBody String param) {
        return new PatientInfo().getPatientInfo(param);
    }
    /*
     * 标题栏、患者基本信息
     */
    @RequestMapping(value = "/patient_basic_info_detail", method = RequestMethod.POST)
    public
    @ResponseBody
    String getPatientInfoDetail(@RequestBody String param) {
        return new PatientInfo().getPatientInfoDetail(param);
    }

    //遗传疾病
    @RequestMapping(value = "/genetic_disease", method = RequestMethod.POST)
    public
    @ResponseBody
    String getGeneticDisease(@RequestBody String param) {
        return new GeneticDisease().getGeneticDisease(param);
    }

    // 药物反应
    @RequestMapping(value = "/drug_reaction", method = RequestMethod.POST)
    public
    @ResponseBody
    String getDrugReaction(@RequestBody String param) {
        return new DrugReaction().getDrugReaction(param);
    }

    //DC治疗
    @RequestMapping(value = "/dc_order", method = RequestMethod.POST)
    public
    @ResponseBody
    String getDC(@RequestBody String param) {
        return new DCOrders().getDC_orders(param);
    }


	/*
     * 基本图形统计，时间轴过滤
	 * 输入patient_cn
	 * 返回：疾病，就诊类型，就诊科室，就诊时间 四个维度上的就诊次数
	 */

    @RequestMapping(value = "/PatientBasicFigure", method = RequestMethod.POST)
    public
    @ResponseBody
    String getVisitCountByDimension(@RequestBody String param) {
        return new VisitCountByDimension().getVisitCountByDimension(param);
    }


    /*
     * 查看指标变化————指标列表
     * input: patient_sn
     * visit_sn
     * return: 检验指标的中文名称列表
     */
    @RequestMapping(value = "/quota_name_list", method = RequestMethod.POST)
    public
    @ResponseBody
    String getQuotaNameList(@RequestBody String param) {
        return choiceList.getChoiceList(param, ChoiceList.SUB_INSPECTION);
    }
    /**
     * 三测单 指标变化
     * */
    @RequestMapping(value = "/tri_name_list", method = RequestMethod.POST)
    public
    @ResponseBody
    String getTriNameList(@RequestBody String param) {
        return choiceList.getChoiceList(param, ChoiceList.TRI);
    }

    /*
     * 查看指标变化————指标取值列表
     * 输入：
     * 	patient_sn：病人id
     * 	id：检验id
     * 	start_date：开始时间
     * 	end_date：结束时间
     * 输出：start_date和end_date区间内的各个时间点 id的取值
     */
    @RequestMapping(value = "/quota_reports", method = RequestMethod.POST)
    public
    @ResponseBody
    String getQuotaReports(@RequestBody String param) {
        return choiceList.getItemInfo(param, ChoiceList.SUB_INSPECTION);
    }

    /*
     * 查看指标变化————指标取值列表
     * 输入：
     * 	patient_sn：病人id
     * 	id：检验id
     * 	start_date：开始时间
     * 	end_date：结束时间
     * 输出：start_date和end_date区间内的各个时间点 id的取值
     */
    @RequestMapping(value = "/new_quota_reports", method = RequestMethod.POST)
    public
    @ResponseBody
    String getNewQuotaReports(@RequestBody String param) {
        return  new LabResultItem().getNewQuotaReports(param);
    }
    /**
     * 三测单 指标变化
     * */
    @RequestMapping(value = "/tri_reports", method = RequestMethod.POST)
    public
    @ResponseBody
    String getTriReports(@RequestBody String param) {
        return choiceList.getItemInfo(param, ChoiceList.TRI);
    }


    /*
     * 时间轴
     * 输入：patient_id，以及过滤项
     * 输出：就诊基本信息
     */
    @RequestMapping(value = "/visit_timeline", method = RequestMethod.POST)
    public
    @ResponseBody
    String getVisitTimeline(@RequestBody String param) {
        return new PatientBasicTimeAxis().getVisitTimeline(param);
    }

	/*@RequestMapping(value = "/TestVisitTimeline", method = RequestMethod.POST)
    public @ResponseBody String testVisitTimeline(@RequestBody String param) {
		return new PatientBasicTimeAxis().testVisitTimeline(param);
	}*/

    //分类详情——诊断结果
    @RequestMapping(value = "/category_catalog", method = RequestMethod.POST)
    public
    @ResponseBody
    String getCategoryCatalog(@RequestBody String param) {
        return new CategoryCatalog().getCategoryCatalog(param);
    }

    //分类详情——分子诊断
    @RequestMapping(value = "/molecular_detection", method = RequestMethod.POST)
    public
    @ResponseBody
    String getMolecularDetection(@RequestBody String param) {
        return new MergeRequestInLocal(new MolecularDetection(JsonAttrUtil.toJsonObject(param))).getData().toString();
        //return new MolecularDetection().getMolecularDetection(param);
    }
    //组学
    @RequestMapping(value = "/getGennomics", method = RequestMethod.POST)
    @ResponseBody
    public String getGennomics(@RequestBody String param){

        return new GennomicsList().getGennomicsList(param);
    }

    /*
     * 实验室检验——检验项分类列表
     * input:
     * 	patient_cn
     * return：
     * 	visits.inspection_reports.INSPECTION_AIM：检验目的列表
     */
    @RequestMapping(value = "/lab_result_item_list", method = RequestMethod.POST)
    public
    @ResponseBody
    String getLabResultItemList(@RequestBody String param) {
        return new LabResultItemList().getLabResultItemList(param);
    }

    /*
     * 实验室检验——检验子项列表
     * input:
     * 	patient_cn
     * return：
     * 	 visits.inspection_reports.sub_inspection：检验子项记录列表
     */
    @RequestMapping(value = "/lab_result_item", method = RequestMethod.POST)
    public
    @ResponseBody
    String getLabResultItem(@RequestBody String param) {
        return new LabResultItem().getLabResultItem(param);
    }

    /*
     * 实验室检验——新检验子项列表
     * input:
     * 	patient_cn 检验编号
     * return：
     * 	 visits.inspection_reports.sub_inspection：检验子项记录列表
     */
    @RequestMapping(value = "/new_lab_result_item", method = RequestMethod.POST)
    public
    @ResponseBody
    String getNewLabResultItem(@RequestBody String param) {
        return new LabResultItem().getNewLabResultItem(param);
    }


    /*
    * 检验大项 ---检验列表
    * */
    @RequestMapping(value = "/lab_result", method = RequestMethod.POST)
    public
    @ResponseBody
    String getLab_result(@RequestBody String param) {
        return new LabResultItem().getLabResul(param);
//
    }
    //分类详情——检查检验
    @RequestMapping(value = "/exam_result", method = RequestMethod.POST)
    public
    @ResponseBody
    String getExamResult(@RequestBody String param) {
        return new ExamResult().getExamResult(param);
    }

    //分类详情——新检查检验
    @RequestMapping(value = "/new_exam_result", method = RequestMethod.POST)
    public
    @ResponseBody
    String getNewExamResult(@RequestBody String param) {
        return new ExamResult().getNewExamResult(param);
    }

    //骨髓血液细胞检查报告
    @RequestMapping(value = "/bone_marrow_blood_tests_reports", method = RequestMethod.POST)
    public
    @ResponseBody
    String getBoneMarrowBloodReports(@RequestBody String param) {
        return new BoneMarrowBloodReports().getData(param);
    }


    //心电图报告
    @RequestMapping(value = "/electrocardiogram_reports", method = RequestMethod.POST)
    public
    @ResponseBody
    String getElectrocardiogramReports(@RequestBody String param) {
        return new ElectrocardiogramReports().getData(param);
    }

    //治疗--放疗检查 放疗疗程
    @RequestMapping(value = "/remedy", method = RequestMethod.POST)
    public
    @ResponseBody
    String getRemedy(@RequestBody String param) {
        return new Remedy().getData(param);
    }

    //治疗--放疗检查 放疗疗程
    @RequestMapping(value = "/newRemedy", method = RequestMethod.POST)
    public
    @ResponseBody
    String getNewRemedy(@RequestBody String param) {
        return new Remedy().getRemedy(param);
    }

    //分类详情——病理检验
    @RequestMapping(value = "/pathological_examination", method = RequestMethod.POST)
    public
    @ResponseBody
    String getPathologicalExamination(@RequestBody String param) {
        return new PathologyReports().getPathologyReports(param);
    }

    //手术信息
    @RequestMapping(value = "/operation_records", method = RequestMethod.POST)
    public
    @ResponseBody
    String getOperationRecords(@RequestBody String param) {
        return new OperationRecords().getOperationRecords(param);
    }

    //新手术信息
    @RequestMapping(value = "/new_operation_records", method = RequestMethod.POST)
    public
    @ResponseBody
    String getNewOperationRecords(@RequestBody String param) {
        return new OperationRecords().getNewOperationRecords(param);
    }
    //放疗
    @RequestMapping(value = "/radiotherapy", method = RequestMethod.POST)
    public
    @ResponseBody
    String getRadiotherapy(@RequestBody String param) {
        return new Radiotherapy().getRadiotherapy(param);
    }

    //化疗信息
    @RequestMapping(value = "/chemotherapy_info", method = RequestMethod.POST)
    public
    @ResponseBody
    String getChemotherapy(@RequestBody String param) {
        return new Chemotherapy().getChemotherapy(param);
    }

    //用药
    @RequestMapping(value = "/pharmacy", method = RequestMethod.POST)
    public
    @ResponseBody
    String getPharmacyOrder(@RequestBody String param) {
        return new PharmarcyOrder().getPharmarcyOrder(param);
    }

    // 新 药品遗嘱
    @RequestMapping(value = "/newPharmacy", method = RequestMethod.POST)
    public
    @ResponseBody
    String getNewPharmacyOrder(@RequestBody String param) {
        return new PharmarcyOrder().getNewPharmarcyOrder(param);
    }

    // 新 非药品遗嘱
    @RequestMapping(value = "/new_Orders_Pharmacy", method = RequestMethod.POST)
    public
    @ResponseBody
    String getNewOrdersPharmacy(@RequestBody String param) {
        return new PharmarcyOrder().getNewOrdersPharmacy(param);
    }
    // 新 单独每天搜索 分页 sd功能
    @RequestMapping(value = "/orders_pharmacy_day", method = RequestMethod.POST)
    public
    @ResponseBody
    String getOrdersPharmacyDay(@RequestBody String param) {
        return new PharmarcyOrder().getOrdersPharmacyDay(param);
    }
    //入院记录
    @RequestMapping(value = "/admission_records", method = RequestMethod.POST)
    public
    @ResponseBody
    String getAdmissionRecords(@RequestBody String param) {

        return new AdmissionRecords().getAdmissionRecords(param);
    }

    //出院记录
    @RequestMapping(value = "/discharge_records", method = RequestMethod.POST)
    public
    @ResponseBody
    String getDischargeRecords(@RequestBody String param) {

        return new DischargeRecords().getDischargeRecords(param);
    }

    //首次病程
    @RequestMapping(value = "/first_course", method = RequestMethod.POST)
    public
    @ResponseBody
    String getFirstCourseRecord(@RequestBody String param) {

        return new FirstCourseRecord().getFirstCourseRecord(param);
    }

    //病程记录
    @RequestMapping(value = "/course_records", method = RequestMethod.POST)
    public
    @ResponseBody
    String getCourseRecord(@RequestBody String param) {

        return new CourseRecord().getCourseRecord(param);
    }

    //新病程记录
    @RequestMapping(value = "/new_course_records", method = RequestMethod.POST)
    public
    @ResponseBody
    String getNewCourseRecord(@RequestBody String param) {
        return new CourseRecord().getNewCourseRecord(param);
    }

    //生物标本
    @RequestMapping(value = "/biological_specimen", method = RequestMethod.POST)
    public
    @ResponseBody
    String getSpecimenInfo(@RequestBody String param) {

        return new SpecimenInfo().getSpecimenInfo(param);
    }

    //病案首页
    @RequestMapping(value = "/medical_records", method = RequestMethod.POST)
    public
    @ResponseBody
    String getMedicalRecord(@RequestBody String param) {

        return new MedicalRecord().getMedicalRecord(param);
    }

    //病历文书
    @RequestMapping(value = "/medical_course", method = RequestMethod.POST)
    public
    @ResponseBody
    String getMedicalCourseText(@RequestBody String param) {

        return new MedicalCourseText().getMedicalCourseText(param);
    }

    //病历文书 - 新
    @RequestMapping(value = "/new_medical_course", method = RequestMethod.POST)
    public
    @ResponseBody
    String getNewMedicalCourseText(@RequestBody String param) {

        return new MedicalCourseText().getNewMedicalCourseText(param);
    }


    //镜检
    @RequestMapping(value = "/endoscope_record", method = RequestMethod.POST)
    public
    @ResponseBody
    String getEndoscopeRecord(@RequestBody String param) {

        return new EndoscopeRecord().getEndoscopeRecord(param);
    }

    //三测单
    @RequestMapping(value = "/triple_test_table", method = RequestMethod.POST)
    public
    @ResponseBody
    String getTripleTestTable(@RequestBody String param) {
        return new TripleTestTable().getTripleTestTable(param);
    }

    //新三测单
    @RequestMapping(value = "/tripleTestTable", method = RequestMethod.POST)
    public
    @ResponseBody
    String getNewsTripleTestTable(@RequestBody String param) {
        try {
            return new TripleTestTable().getNewsTripleTestTable(param);
        }catch (Exception e){
            return ResponseMsgFactory.buildFailStr("no data");
        }
    }

    //普通护理记录
    @RequestMapping(value = "/general_nursing_record", method = RequestMethod.POST)
    public
    @ResponseBody
    String general_nursing_record(@RequestBody String param) {
        EachVisitResponse vt = new EachVisitResponse(new String[]{
                "general_nursing_record"
        },
                new String[]{
                        "general_nursing_record" //普通护理记录
                }
        );
        return ResponseMsgFactory.getResponseStr(vt, param);
    }

    //手术护理记录
    @RequestMapping(value = "/operation_nursing_record", method = RequestMethod.POST)
    public
    @ResponseBody
    String operation_nursing_record(@RequestBody String param) {
        EachVisitResponse vt = new EachVisitResponse(new String[]{
                "operation_nursing_record"
        },
                new String[]{
                        "operation_nursing_record"
                }
        );
        return ResponseMsgFactory.getResponseStr(vt, param);
    }

    /**
     * 缩略图
     */
    @RequestMapping(value = "/thumbnail", method = RequestMethod.GET)
    public void getThumbnail(@RequestParam("url") String url, HttpServletResponse response) {
        new Thumbnail().getImage(url, response);
    }

    /**
     * 前端请求，缓存病人
     */
    @RequestMapping(value = "/for_cache", method = RequestMethod.POST)
    public
    @ResponseBody
    String for_cache(@RequestBody String param) {
        return new ForCache().getData(param);
    }

    @RequestMapping(value = "/get_patien_sn", method = RequestMethod.POST)
    @ResponseBody
    public String get_patien_sn(@RequestBody String param){
        logger.info("this request from HIS");
        logger.info("param = {}",param);
        String empiInfo = empiService.getEMPIInfo(param);
        String uuids = empiService.patientSN(empiInfo, "Uuids");
        logger.info("uuid: " + uuids);
        return uuids;
    }

    @RequestMapping(value = "/get_many_basic_info", method = RequestMethod.POST)
    @ResponseBody
    public String get_many_basic_info(@RequestBody String param){
        logger.info("一个病案号对应多个病人的信息");
        return new PatientInfo().getManyPatientInfo(param);
    }

    //泳道图
    @RequestMapping(value = "/swimlane", method = RequestMethod.POST)
    public
    @ResponseBody
    String getSwimlane(@RequestBody String param) {
        return new SwimlaneService().getSwimlane(param);
    }

    //病案首页
    @RequestMapping(value = "/newMedical_records", method = RequestMethod.POST)
    public
    @ResponseBody
    String getNewMedicalRecord(@RequestBody String param) {
        return new MedicalRecord().getNewMedicalRecord(param);
    }

}
