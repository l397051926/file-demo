package com.gennlife.fs.configurations.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.darren.collection.string.Matching;
import com.gennlife.fs.common.utils.KeyPathUtil;
import com.gennlife.fs.configurations.ModelVersion;
import com.gennlife.fs.configurations.model.conversion.ModelConverter;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

import static com.gennlife.fs.common.utils.FilesUtils.readFile;
import static com.gennlife.fs.common.utils.KeyPathUtil.toKeyPath;
import static com.gennlife.fs.common.utils.TypeUtil.BV;
import static com.gennlife.fs.common.utils.TypeUtil.S;
import static com.gennlife.fs.configurations.model.DataType.DATE;
import static com.gennlife.fs.configurations.model.Model.*;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class ModelLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelLoader.class);

    public static void load(String modelName, PropertyResolver props) throws IOException {
        val model = new Model();
        model._name = modelName;
        val enabled = BV(props.getProperty("enabled"));
        if (enabled) {
            model._displayName = S(props.getProperty("display-name"));
            model._sourceType = SourceType.fromString(S(props.getProperty("source")));
            if ("custom".equals(modelName)) {
                CUSTOM_MODEL_DISPLAY_NAME = model._displayName;
                CUSTOM_MODEL_SOURCE_TYPE = model._sourceType;
            } else {
                model._predefined = true;
                model._version = new ModelVersion(S(props.getProperty("version")));
                model._allFieldInfo = new HashMap<>();
                val jsonStr = readFile("/configurations/model/" + modelName + "/definition/" + model._version + ".json");
                _loadModelBody(model, JSON.parseObject(jsonStr), new KeyPath(), new KeyPath());
                model._indexName = S(props.getProperty("index-name"));
                model._rwsName = S(props.getProperty("rws-name"));
                model._patientSnField = toKeyPath(S(props.getProperty("patient-sn-field")));
                model._partitionGroup = toKeyPath(S(props.getProperty("partition-group")));
                if (model._partitionGroup != null) {
                    model._partitionField = toKeyPath(S(props.getProperty("partition-field")));
                    model._sortFields = Stream.of(S(props.getProperty("sort-fields")).split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(KeyPathUtil::toKeyPath)
                        .collect(toMap(
                            identity(),
                            path -> {
                                val field = model._partitionGroup.keyPathByAppending(path);
                                val info = model.fieldInfo(field);
                                if (info == null) {
                                    throw new RuntimeException("在模型 " + model + " 中未找到设定的排序字段：" + field);
                                }
                                return info;
                            },
                            (a, b) -> a,
                            LinkedHashMap::new));
                }
                val cvtProfile = S(props.getProperty("conversion-profile"));
                if (!Matching.isEmpty(cvtProfile)) {
                    val cvtJsonStr = readFile("/configurations/model/" + modelName + "/conversion/" + cvtProfile + ".json");
                    val cvtJson = JSON.parseArray(cvtJsonStr);
                    model._converter = new ModelConverter(cvtJson);
                }
                MODELS.put(modelName, model);
                if (model._rwsName != null) {
                    MODELS_RWS.put(model._rwsName, model);
                }
            }
            LOGGER.info("已加载模型：" + model);
        }
    }

    private static void _loadModelBody(Model model, JSONObject json, KeyPath path, KeyPath displayPath) {
        val children = json.getJSONObject("children");
        if (children != null) {
            for (val key : children.keySet()) {
                val sub = children.getJSONObject(key);
                path.addLast(key);
                displayPath.addLast(sub.getString("displayName"));
                _loadModelBody(model, sub, path, displayPath);
                path.removeLast();
                displayPath.removeLast();
            }
        } else {
            val field = path.clone();
            val info = FieldInfo.builder()
                .path(field)
                .displayPath(displayPath.clone())
                .type(DataType.fromString(json.getString("type")))
                .build();
            if (DATE.equals(info.type)) {
                info.dateFormat = json.getString("format");
                info.dateFormatter = DateTimeFormatter.ofPattern(info.dateFormat);
            }
            model._allFieldInfo.put(field, info);
        }
    }

}
