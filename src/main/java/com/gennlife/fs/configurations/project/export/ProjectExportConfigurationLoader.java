package com.gennlife.fs.configurations.project.export;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.fs.configurations.ModelVersion;
import com.gennlife.fs.configurations.model.Model;
import com.gennlife.fs.configurations.model.ProjectExportFieldInfo;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.gennlife.fs.common.utils.FilesUtils.readFile;
import static com.gennlife.fs.configurations.model.Model.modelByName;

public class ProjectExportConfigurationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectExportConfigurationLoader.class);

    public static void load(ModelVersion cfgVersion) throws IOException {
        if (cfgVersion == null) {
            return;
        }
        val jsonStr = readFile("/configurations/project-export/model/" + cfgVersion + ".json");
        val json = JSON.parseObject(jsonStr);
        val top = json.getJSONObject("children");
        for (val modelName : top.keySet()) {
            val model = modelByName(modelName);
            if (model == null) {
                LOGGER.error("无法找到名为 " + modelName + " 的模型");
                continue;
            }
            _loadModel(model, top.getJSONObject(modelName), new KeyPath());
            LOGGER.info("已成功读取模型 " + model + " 的数据导出配置");
        }
    }

    private static void _loadModel(Model model, JSONObject json, KeyPath path) {
        val children = json.getJSONObject("children");
        if (children != null) {
            for (val key : children.keySet()) {
                val sub = children.getJSONObject(key);
                path.addLast(key);
                _loadModel(model, sub, path);
                path.removeLast();
            }
        } else {
            try {
                model.fieldInfo(path).projectExport = ProjectExportFieldInfo.builder()
                    .index(json.getInteger("index"))
                    .mergeCells(json.getBooleanValue("mergeCells"))
                    .selectedByDefault(json.getBooleanValue("selectedByDefault"))
                    .sorted(json.getBooleanValue("sorted"))
                    .build();
            } catch (NullPointerException e) {
                LOGGER.warn("在模型 " + model + " 中未找到字段：" + path);
            }
        }
    }

}
