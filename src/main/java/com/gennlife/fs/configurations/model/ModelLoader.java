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

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.gennlife.fs.common.utils.FilesUtils.readFile;
import static com.gennlife.fs.common.utils.KeyPathUtil.toKeyPath;
import static com.gennlife.fs.common.utils.TypeUtil.BV;
import static com.gennlife.fs.common.utils.TypeUtil.S;
import static com.gennlife.fs.configurations.model.DataType.DATE;
import static com.gennlife.fs.configurations.model.Model.MODELS;
import static com.gennlife.fs.configurations.model.Model.MODELS_RWS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class ModelLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelLoader.class);

    public static void load(String modelName, Map<String, Object> props) throws IOException {
        val model = new Model();
        model._name = modelName;
        val enabled = BV(props.get("enabled"));
        if (enabled) {
            model._version = new ModelVersion(S(props.get("version")));
            model._sourceType = SourceType.fromString(S(props.get("source")));
            model._allFieldInfo = new HashMap<>();
            val jsonStr = readFile("/configurations/model/" + modelName + "/definition/" + model._version + ".json");
            _loadModelBody(model, JSON.parseObject(jsonStr), new KeyPath(), new KeyPath());
            model._indexName = S(props.get("index-name"));
            model._rwsName = S(props.get("rws-name"));
            model._displayName = S(props.get("display-name"));
            model._patientSnField = toKeyPath(S(props.get("patient-sn-field")));
            model._partitionGroup = toKeyPath(S(props.get("partition-group")));
            if (model._partitionGroup != null) {
                model._partitionField = toKeyPath(S(props.get("partition-field")));
                model._projectExportSortFields = Stream.of(S(props.get("sort-fields")).split(","))
                    .map(String::trim)
                    .filter(String::isEmpty)
                    .map(KeyPathUtil::toKeyPath)
                    .collect(toMap(identity(), model::fieldInfo, (a, b) -> a, LinkedHashMap::new));
            }
            val cvtProfile = S(props.get("conversion-profile"));
            if (!Matching.isEmpty(cvtProfile)) {
                val cvtJsonStr = readFile("/configurations/model/" + modelName + "/conversion/" + cvtProfile + ".json");
                val cvtJson = JSON.parseArray(cvtJsonStr);
                model._converter = new ModelConverter(cvtJson);
            }
            MODELS.put(modelName, model);
            MODELS_RWS.put(model._rwsName, model);
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
