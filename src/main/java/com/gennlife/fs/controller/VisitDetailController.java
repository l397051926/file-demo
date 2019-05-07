package com.gennlife.fs.controller;

import com.gennlife.fs.common.cache.MergeRequestInLocal;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.service.patientsdetail.serviceitem.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 单次就诊详情浏览Controller
 */
@Controller
@RequestMapping(value="/VisitDetail", produces = "application/json;charset=UTF-8")
public class VisitDetailController {

	/*
	* 单次就诊基本信息——标题栏
	* input: patient_sn, visit_sn
	* output: visit_info
	*/	
	@RequestMapping(value = "/visit_detail_info", method = RequestMethod.POST)
	public @ResponseBody
	String getVisitDetail(@RequestBody String param) {
		VisitDetail vd = new VisitDetail();
		return vd.getVisitDetail(param);
	}

	/*
	* 单次就诊详情——入院记录
	* input: patient_sn, visit_sn
	* output: admission_records
	*/	
	@RequestMapping(value = "/admission_records", method = RequestMethod.POST)
	public @ResponseBody
	String getAdmissionRecords(@RequestBody String param) {
		AdmissionRecords ar = new AdmissionRecords();
		return ar.getAdmissionRecords(param);
	}
	
	
	//诊断
	@RequestMapping(value = "/diagnose", method = RequestMethod.POST)
	public @ResponseBody
	String getVisitDiagnose(@RequestBody String param) {
		DiagnoseRecords dr = new DiagnoseRecords();
		return dr.getDiagnoseRecords(param);
	}
	//新诊断
	@RequestMapping(value = "/newDiagnose", method = RequestMethod.POST)
	public @ResponseBody
	String getVisitNewDiagnose(@RequestBody String param) {
		DiagnoseRecords dr = new DiagnoseRecords();
		return dr.getNewDiagnoseRecords(param);
	}
	
	//分子检测
	@RequestMapping(value = "/molecular_detection", method = RequestMethod.POST)
	public @ResponseBody
	String getMolecularDetection(@RequestBody String param) {
		return new MergeRequestInLocal(new MolecularDetection(JsonAttrUtil.toJsonObject(param))).getData().toString();
		/*MolecularDetection md = new MolecularDetection();
		return md.getMolecularDetection(param);*/
	}

	/*
	 * 实验室检验——检验项分类列表
	 * input: 
	 * 	patient_cn
	 * return：
	 * 	visits.inspection_reports.INSPECTION_AIM：检验目的列表
	 */
	@RequestMapping(value = "/lab_result_item_list", method = RequestMethod.POST)
	public @ResponseBody
	String getLabResultItemList(@RequestBody String param) {
		LabResultItemList lri_list = new LabResultItemList();
		return lri_list.getLabResultItemList(param);
	}
	
	/*
	 * 实验室检验——检验子项列表
	 * input: 
	 * 	patient_cn
	 * return：
	 * 	 visits.inspection_reports.sub_inspection：检验子项记录列表
	 */
	@RequestMapping(value = "/lab_result_item", method = RequestMethod.POST)
	public @ResponseBody
	String getLabResultItem(@RequestBody String param) {
		LabResultItem lri = new LabResultItem();
		return lri.getLabResultItem(param);
	}

	//分类详情——检查检验——超声、影像、镜检、肺功能、细胞病理学
	@RequestMapping(value = "/exam_result", method = RequestMethod.POST)
	public @ResponseBody
	String getExamResult(@RequestBody String param) {
		ExamResult exam_result = new ExamResult();
		return exam_result.getExamResult(param);
	}

	//分类详情——病理检验
	@RequestMapping(value = "/pathological_examination", method = RequestMethod.POST)
	public @ResponseBody
	String getPathologicalExamination(@RequestBody String param) {
		PathologyReports patho_rp = new PathologyReports();
		return patho_rp.getPathologyReports(param);
	}

	//手术信息
	@RequestMapping(value = "/operation_records", method = RequestMethod.POST)
	public @ResponseBody
	String getOperationRecords(@RequestBody String param) {
		OperationRecords operation_records = new OperationRecords();
		return operation_records.getOperationRecords(param);
	}
	
	//放疗
	@RequestMapping(value = "/radiotherapy", method = RequestMethod.POST)
	public @ResponseBody
	String getRadiotherapy(@RequestBody String param) {
		Radiotherapy rt = new Radiotherapy();
		return rt.getRadiotherapy(param);
	}
	
	//化疗信息
	@RequestMapping(value = "/chemotherapy_info", method = RequestMethod.POST)
	public @ResponseBody
	String getChemotherapy(@RequestBody String param) {
		Chemotherapy cb = new Chemotherapy();
		return cb.getChemotherapy(param);
	}
	
	//用药
	@RequestMapping(value = "/pharmacy", method = RequestMethod.POST)
	public @ResponseBody
	String getPharmacyOrder(@RequestBody String param) {
		PharmarcyOrder po = new PharmarcyOrder();
		return po.getPharmarcyOrder(param);
	}
	
	//出院记录
	@RequestMapping(value = "/discharge_records", method = RequestMethod.POST)
	public @ResponseBody
	String getDischargeRecords(@RequestBody String param) {
		DischargeRecords dr = new DischargeRecords();
		return dr.getDischargeRecords(param);
	}
	
	//首次病程
	@RequestMapping(value = "/first_course", method = RequestMethod.POST)
	public @ResponseBody
	String getFirstCourseRecord(@RequestBody String param) {
		FirstCourseRecord fcr = new FirstCourseRecord();
		return fcr.getFirstCourseRecord(param);
	}
	
	//病历记录
	@RequestMapping(value = "/course_records", method = RequestMethod.POST)
	public @ResponseBody
	String getCourseRecord(@RequestBody String param) {
		CourseRecord cr = new CourseRecord();
		return cr.getCourseRecord(param);
	}
	//DC治疗
	@RequestMapping(value = "/dc_order", method = RequestMethod.POST)
	public @ResponseBody
	String getDC(@RequestBody String param) {
		DCOrders dc = new DCOrders();
		return dc.getDC_orders(param);
	}
	//三测单
	@RequestMapping(value = "/triple_test_table", method = RequestMethod.POST)
	public @ResponseBody
	String getTripleTestTable(@RequestBody String param) {

		return new TripleTestTable().getTripleTestTable(param);
	}

}
