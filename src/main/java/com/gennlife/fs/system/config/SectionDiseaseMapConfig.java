package com.gennlife.fs.system.config;

import com.gennlife.fs.common.utils.JsonAttrUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 科室疾病对应关系
 */
@Component
@ConfigurationProperties(prefix = "fs.section")
@Scope("singleton")
public class SectionDiseaseMapConfig implements InitializingBean {
    private static JsonObject mapdata = null;
    private static final Logger logger = LoggerFactory.getLogger(SectionDiseaseMapConfig.class);

    public static void clear() {
        mapdata = null;
    }

    private List<SectionItemEntity> sections;
    /**
     * 获取科室对应的疾病编号
     *
     * @parm sectioncode 科室代号
     */
    public static JsonArray getAllCode(String sectioncode) {
        if (mapdata == null) return null;
        if (mapdata.has(sectioncode))
            return mapdata.getAsJsonObject(sectioncode).getAsJsonArray("ICD");
        return null;
    }

    public static boolean checkSectionName(String section_name) {
        if (mapdata == null) return false;
        return mapdata.has(section_name.toUpperCase());
    }

    public static boolean isBelong(JsonArray codearray, String section_name) {
        boolean flag = false;
        if (codearray == null || codearray.size() < 0) return flag;
        for (JsonElement code : codearray) {
            flag = isBelong(code.getAsString(), getAllCode(section_name));
            if (flag == true) break;
        }
        return flag;
    }

    private static boolean isBelong(String code, JsonArray mapStr) {
        boolean flag = false;
        if (mapStr == null) return flag;
        code = code.replaceAll("\"", "");
        code = code.replace(" ", ".");
        for (Object item : mapStr) {
            String value = item.toString().replaceAll("\"", "");
            if (code.startsWith(value)) {
                flag = true;
                break;
            }

        }
        return flag;
    }

    public List<SectionItemEntity> getSections() {
        return sections;
    }

    public void setSections(List<SectionItemEntity> sections) {
        this.sections = sections;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            mapdata = new JsonObject();
            for (SectionItemEntity sectionItemEntity : sections) {
                JsonObject item = new JsonObject();
                item.add("ICD", JsonAttrUtil.toJsonTree(sectionItemEntity.getIcd().split(";")));
                mapdata.add(sectionItemEntity.getName(), item);
            }
            logger.info("map data " + mapdata);
        } catch (Exception e) {
            logger.error("section map Exception ", e);
            mapdata = null;
        }
        if (mapdata == null || mapdata.entrySet().size() == 0) {
            logger.error("科室疾病配置失败");
            return;
        } else {
            logger.info("科室疾病配置成功");
            return;
        }
    }
}
