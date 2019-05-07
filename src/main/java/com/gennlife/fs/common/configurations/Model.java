package com.gennlife.fs.common.configurations;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.Pair;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.darren.collection.keypath.KeyPathSet;
import com.gennlife.fs.service.ProjectService;
import lombok.val;
import lombok.var;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.gennlife.darren.collection.Pair.makePair;
import static com.gennlife.fs.common.utils.KeyPathUtil.toPathString;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

public class Model {

    public static final String EMR_MODEL_NAME = "emr";
    public static final String CUSTOM_MODEL_NAME = "custom";
    public static String CUSTOM_MODEL_DISPLAY_NAME = null;

    public static Model emrModel() {
        return modelByName(EMR_MODEL_NAME);
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
        _allFieldInfo = IntStream.range(0, vars.size())
            .mapToObj(i -> {
                val v = vars.get(i);
                return FieldInfo.builder()
                    .groupOrdinal(0)
                    .fieldOrdinal(i)
                    .index(i)
                    .path(new KeyPath(v.id))
                    .displayPath(new KeyPath(v.name))
                    .exportSupported(true)
                    .mergeCells(true)
                    .selectedByDefault(true)
                    .sorted(false)
                    .build();
            })
            .collect(toMap(info -> info.path, identity(), (a, b) -> a, LinkedHashMap::new));
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

    public KeyPath patientSnField() {
        return _patientSnField;
    }

    public KeyPath partitionGroup() {
        return _partitionGroup;
    }

    public KeyPath partitionField() {
        return _partitionField;
    }

    public KeyPath sortField() {
        return _sortField;
    }

    public KeyPathSet mergedFields() {
        return _mergedFields;
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
        return _fields.isEmpty();
    }

    public Map<KeyPath, FieldInfo> allFieldInfo() {
        return _allFieldInfo;
    }

    public KeyPathSet fields() {
        return _fields;
    }

    public FieldInfo fieldInfo(Comparable ...keyOrKeyPaths) {
        return _allFieldInfo.get(new KeyPath(keyOrKeyPaths));
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

    // requires (name, displayName, allFieldInfo)
    void generateCaches() {
        _fields = new KeyPathSet(_allFieldInfo.keySet(), LinkedHashMap::new);
        _mergedFields = _allFieldInfo
            .values()
            .stream()
            .filter(info -> info.mergeCells)
            .map(info -> info.path)
            .collect(toCollection(KeyPathSet::new));
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
        _frontEndObject = toFrontEndObject(new KeyPath(), _fields);
    }

    private JSONObject toFrontEndObject(KeyPath path, KeyPathSet set) {
        val ret = new JSONObject();
        if (_fields.contains(path)) {
            if (_allFieldInfo.get(path).exportSupported) {
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

    boolean _predefined;
    KeyPathSet _fields;
    Map<KeyPath, FieldInfo> _allFieldInfo;
    Map<KeyPath, KeyPath> _pathDictionary;
    KeyPath _patientSnField;
    KeyPath _partitionGroup;
    KeyPath _partitionField;
    KeyPath _sortField;
    KeyPathSet _mergedFields;
    String _name;
    String _rwsName;
    String _indexName;
    String _displayName;
    JSONObject _frontEndObject;

    static final Map<String, Model> MODELS = new HashMap<>();
    static final Map<String, Model> MODELS_RWS = new HashMap<>();

    private static final String FRONT_END_TITLE_FIELD = "title";
    private static final String FRONT_END_KEY_FIELD = "key";
    private static final String FRONT_END_CHILDREN_FIELD = "children";

}
