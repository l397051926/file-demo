package com.gennlife.fs.configurations.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.Pair;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.darren.collection.keypath.KeyPathSet;
import com.gennlife.fs.configurations.ModelVersion;
import com.gennlife.fs.configurations.model.conversion.ModelConverter;
import com.gennlife.fs.service.ProjectService;
import lombok.val;
import lombok.var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.gennlife.darren.collection.Pair.makePair;
import static com.gennlife.fs.common.utils.KeyPathUtil.toPathString;
import static java.util.Comparator.comparingInt;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class Model {

    private static final Logger LOGGER = LoggerFactory.getLogger(Model.class);

    public static final String EMR_MODEL_NAME = "emr";
    public static final String PRIVACY_MODEL_NAME = "privacy";
    public static final String CUSTOM_MODEL_NAME = "custom";

    public static String CUSTOM_MODEL_DISPLAY_NAME = null;
    public static SourceType CUSTOM_MODEL_SOURCE_TYPE = null;

    public static Model emrModel() {
        return modelByName(EMR_MODEL_NAME);
    }

    public static Model privacyModel() {
        return modelByName(PRIVACY_MODEL_NAME);
    }

    public static Model modelByName(String modelName) {
        return MODELS.get(modelName);
    }

    public static Model modelByRwsName(String rwsName) {
        return MODELS_RWS.get(rwsName);
    }

    public Model(List<ProjectService.ProjectInfo.CustomVariable> vars) {
        _predefined = false;
        _name = CUSTOM_MODEL_NAME;
        _displayName = CUSTOM_MODEL_DISPLAY_NAME;
        _sourceType = CUSTOM_MODEL_SOURCE_TYPE;
        _allFieldInfo = IntStream.range(0, vars.size())
            .mapToObj(i -> {
                val v = vars.get(i);
                return FieldInfo.builder()
                    .path(new KeyPath(v.id))
                    .displayPath(new KeyPath(v.name))
                    .projectExport(ProjectExportFieldInfo.builder()
                        .index(i)
                        .mergeCells(true)
                        .selectedByDefault(true)
                        .sorted(false)
                        .build())
                    .build();
            })
            .collect(toMap(info -> info.path, identity(), (a, b) -> a));
        generateCaches();
    }

    public String name() {
        return _name;
    }

    public String rwsName() {
        return _rwsName;
    }

    public String indexName() {
        return _indexName;
    }

    public String displayName() {
        return _displayName;
    }

    public ModelVersion version() {
        return _version;
    }

    public ModelConverter converter() {
        return _converter;
    }

    public SourceType sourceType() {
        return _sourceType;
    }

    public KeyPath patientSnField() {
        return _patientSnField;
    }

    public KeyPath partitionGroup() {
        return _partitionGroup;
    }

    public KeyPath partitionField() {
        return _partitionField;
    }

    public boolean isEMR() {
        return EMR_MODEL_NAME.equals(_name);
    }

    public boolean isCRF() {
        return isPredefined() && !isEMR();
    }

    public boolean isPredefined() {
        return _predefined;
    }

    public boolean isCustom() {
        return CUSTOM_MODEL_NAME.equals(_name);
    }

    public boolean isPartitioned() {
        return _partitionGroup != null;
    }

    public boolean isEmpty() {
        return _allPaths.isEmpty();
    }

    public FieldInfo fieldInfo(Comparable ...keyOrKeyPaths) {
        return _allFieldInfo.get(new KeyPath(keyOrKeyPaths));
    }

    public KeyPathSet allPaths() {
        return _allPaths;
    }

    public Map<KeyPath, FieldInfo> allFieldInfo() {
        return _allFieldInfo;
    }

    public Map<KeyPath, FieldInfo> projectExportFields() {
        return _projectExportFields;
    }

    public Map<KeyPath, FieldInfo> projectExportSelectByDefaultFields() {
        return _projectExportSelectByDefaultFields;
    }

    public Map<KeyPath, FieldInfo> projectExportSortFields() {
        return _projectExportSortFields;
    }

    public Map<KeyPath, FieldInfo> projectExportMergedFields() {
        return _projectExportMergedFields;
    }

    public Map<KeyPath, KeyPath> pathDictionary() {
        return _pathDictionary;
    }

    public JSONObject toFrontEndObject() {
        return _frontEndObject;
    }

    Model() {
        _predefined = true;
    }

    public static void generateCachesForAllModels() {
        MODELS.values().forEach(model -> {
            model.generateCaches();
            LOGGER.info("已成功为模型 " + model + " 生成了缓存成员");
        });
    }

    // requires (name, displayName, allFieldInfo)
    private void generateCaches() {
        _allPaths = new KeyPathSet(_allFieldInfo.keySet());
        _projectExportFields = _allFieldInfo
            .entrySet()
            .stream()
            .filter(e -> e.getValue().supportsProjectExport())
            .sorted(comparingInt(e -> e.getValue().projectExport.index))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        _projectExportSelectByDefaultFields = _projectExportFields
            .entrySet()
            .stream()
            .filter(e -> e.getValue().projectExport.selectedByDefault)
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        _projectExportMergedFields = _projectExportFields
            .entrySet()
            .stream()
            .filter(e -> e.getValue().projectExport.mergeCells)
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        _pathDictionary = _allFieldInfo
            .values()
            .stream()
            .flatMap(info -> {
                val s = Stream.<Pair<KeyPath, KeyPath>>builder();
                var a = info.path;
                var b = info.displayPath;
                while (!a.isEmpty()) {
                    s.add(makePair(a, b));
                    a = a.keyPathByRemovingLast();
                    b = b.keyPathByRemovingLast();
                }
                return s.build();
            })
            .collect(toMap(Pair::key, Pair::value, (a, b) -> a));
        _frontEndObject = toFrontEndObject(new KeyPath(), _allPaths);
    }

    private JSONObject toFrontEndObject(KeyPath path, KeyPathSet set) {
        val ret = new JSONObject();
        if (_allPaths.contains(path)) {
            if (_allFieldInfo.get(path).supportsProjectExport()) {
                ret
                    .fluentPut(FRONT_END_TITLE_FIELD, toPathString(new KeyPath(_displayName, _pathDictionary.get(path))))
                    .fluentPut(FRONT_END_KEY_FIELD, toPathString(new KeyPath(_name, path)));
            }
        } else {
            if (!set.isEmpty()) {
                val arr = new JSONArray();
                for (val key: set.subKeys()) {
                    val child = toFrontEndObject(path.keyPathByAppending(key), set.subSet((String)key));
                    if (child != null) {
                        arr.add(child);
                    }
                }
                if (!arr.isEmpty()) {
                    ret
                        .fluentPut(FRONT_END_TITLE_FIELD, toPathString(new KeyPath(_displayName, _pathDictionary.get(path))))
                        .fluentPut(FRONT_END_KEY_FIELD, toPathString(new KeyPath(_name, path)))
                        .fluentPut(FRONT_END_CHILDREN_FIELD, arr);
                }
            }
        }
        return ret.isEmpty() ? null : ret;
    }

    @Override
    public String toString() {
        return _name + " (" + _version + ")";
    }

    boolean _predefined;
    String _name;
    String _rwsName;
    String _indexName;
    String _displayName;
    ModelVersion _version;
    ModelConverter _converter;
    SourceType _sourceType;
    KeyPathSet _allPaths;
    Map<KeyPath, FieldInfo> _allFieldInfo;
    Map<KeyPath, FieldInfo> _projectExportFields;
    Map<KeyPath, FieldInfo> _projectExportSelectByDefaultFields;
    Map<KeyPath, FieldInfo> _projectExportSortFields;
    Map<KeyPath, FieldInfo> _projectExportMergedFields;
    Map<KeyPath, KeyPath> _pathDictionary;
    KeyPath _patientSnField;
    KeyPath _partitionGroup;
    KeyPath _partitionField;
    JSONObject _frontEndObject;

    static final Map<String, Model> MODELS = new HashMap<>();
    static final Map<String, Model> MODELS_RWS = new HashMap<>();

    private static final String FRONT_END_TITLE_FIELD = "title";
    private static final String FRONT_END_KEY_FIELD = "key";
    private static final String FRONT_END_CHILDREN_FIELD = "children";

}
