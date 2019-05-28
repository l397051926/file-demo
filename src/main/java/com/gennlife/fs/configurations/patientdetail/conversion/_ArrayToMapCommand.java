package com.gennlife.fs.configurations.patientdetail.conversion;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gennlife.darren.controlflow.for_.Foreach.foreach;
import static com.gennlife.darren.controlflow.for_.ForeachJSON.foreachKeyPath;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@_CommandName("array to map")
public class _ArrayToMapCommand extends _Command {

    KeyPath fromGroup;
    KeyPath toGroup;
    KeyPath itemPath;
    List<_From> froms;
    Map<String, _To> tos;

    static _Command compile(JSONObject args) {
        _ArrayToMapCommand ret = new _ArrayToMapCommand();
        ret.fromGroup = KeyPath.compile(args.getString("from"));
        ret.toGroup = KeyPath.compile(args.getString("to"));
        ret.itemPath = KeyPath.compile(args.getString("item"));
        ret.froms = args.getJSONArray("sources")
            .stream()
            .map(JSONObject.class::cast)
            .map(value -> {
                _From f = new _From();
                f.field = KeyPath.compile(value.getString("field"));
                f.type = _EMRDataType.valueOf(value.getString("type").toUpperCase());
                return f;
            })
            .collect(toList());
        ret.tos = args.getJSONObject("targets")
            .entrySet()
            .stream()
            .collect(toMap(
                Map.Entry::getKey,
                item -> {
                    _To t = new _To();
                    JSONObject value = (JSONObject)item.getValue();
                    t.field = value.getString("field");
                    t.type = _EMRDataType.valueOf(value.getString("type").toUpperCase());
                    return t;
                }));
        return ret;
    }

    @Override
    boolean run(JSONObject source, JSONObject target) throws Exception {
        final AtomicBoolean ret = new AtomicBoolean(false);
        foreachKeyPath(source, fromGroup, (path, sourceArray) -> {
            if (sourceArray instanceof JSONArray) {
                final KeyPath targetObjectPath = path
                    .keyPathByRemovingLastUntilStringRemains(toGroup.size(), true)
                    .keyPathByReplacingStrings(toGroup);
                // noinspection unchecked
                foreach((List<JSONObject>)sourceArray, sourceObject -> {
                    // noinspection SuspiciousMethodCalls
                    final _To to = tos.get(itemPath.tryResolve(sourceObject));  // null if failed to resolve value
                    if (to != null) {
                        ret.set(true);
                        if (!targetObjectPath.isEmpty()) {
                            targetObjectPath.assignIfAbsent(target, new JSONObject());
                        }
                        final JSONObject targetObject = targetObjectPath.tryResolveAsJSONObject(target);
                        for (_From from: froms) {
                            final Object value = _TypeConversionPlugin.convert(from.field.tryResolve(sourceObject), from.type, to.type);
                            if (value != null) {
                                targetObject.put(to.field, value);
                                break;
                            }
                        }
                    }
                });
            }
        });
        return true;
    }

    private static class _From {
        KeyPath field;
        _EMRDataType type;
    }

    private static class _To {
        String field;
        _EMRDataType type;
    }

}
