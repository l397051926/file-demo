package com.gennlife.fs.system.bean;

import com.gennlife.fs.common.utils.ConfigUtils;
import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.gennlife.fs.common.utils.SystemUtil;
import com.gennlife.fs.system.config.DiseasesKeysConfig;
import com.gennlife.fs.system.config.GenDictConfig;
import com.gennlife.fs.system.config.VisitClassfyConfig;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("DataBean")
@Scope("singleton")
@ConfigurationProperties(prefix = "databean.config")
public class DataBean implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(DataBean.class);
    /**
     * 泳道图配置 科室与病种
     */
    private String sectionMap = null;
    /**
     * 详情页基因数据功能配置
     */
    private String gene = null;
    /**
     * 用于相似病人的配置
     */
    private String similarPatientParam = null;
    /**
     * 用于多病种处理，已废弃，现在病种模型统一
     **/
    private String diseaseKeys = null;
    /**
     * 指标变化配置
     */
    private String indexChangeConfig;
    private static JsonObject indexChangeJson;
    private static JsonObject idMap;
    private static JsonObject originMap;
    /**
     * 泳道图病种与源的对应关系
     **/
    private String visitClassfy = null;

    public String getSectionMap() {
        return sectionMap;
    }

    public void setSectionMap(String sectionMap) {
        this.sectionMap = sectionMap;
    }


    public String getGene() {
        return gene;
    }

    public void setGene(String gene) {
        this.gene = gene;
    }

    public String getSimilarPatientParam() {
        return similarPatientParam;
    }

    public void setSimilarPatientParam(String similarPatientParam) {
        this.similarPatientParam = similarPatientParam;
    }

    public String getDiseaseKeys() {
        return diseaseKeys;
    }

    public void setDiseaseKeys(String diseaseKeys) {
        this.diseaseKeys = diseaseKeys;
    }

    public String getVisitClassfy() {
        return visitClassfy;
    }

    public void setVisitClassfy(String visitClassfy) {
        this.visitClassfy = visitClassfy;
    }

    @Autowired
    public ConfigUtils configUtils;

    @Override
    public void afterPropertiesSet() throws Exception {
        GenDictConfig.init();
        DiseasesKeysConfig.init();
        VisitClassfyConfig.init();
        //配置中心不存在indexChangeConfig.json的时候，从本地拿
        String indexChangeJsonStr = configUtils.getRemoteUtfFile("indexChangeConfig.json");
        indexChangeJson = JsonAttrUtil.toJsonObject(indexChangeJsonStr);
        if (indexChangeJson == null) {
            logger.warn("indexChangeConfig.json 无法从配置中心获取，改从本地获取");
            indexChangeJson = JsonAttrUtil.getJsonObjectfromFile(SystemUtil.getPath(indexChangeConfig));
        }
        if (indexChangeJson == null)
            throw new Exception("indexChangeJson error : file " + SystemUtil.getPath(indexChangeConfig));
        idMap = JsonAttrUtil.getJsonObjectValue("idMap", indexChangeJson);
        originMap = JsonAttrUtil.getJsonObjectValue("originMap", indexChangeJson);
    }


    public static JsonObject getIdMap() {
        return idMap;
    }

    public static JsonObject getOriginMap() {
        return originMap;
    }

    public void setIndexChangeConfig(String indexChangeConfig) {
        this.indexChangeConfig = indexChangeConfig;
    }
}
