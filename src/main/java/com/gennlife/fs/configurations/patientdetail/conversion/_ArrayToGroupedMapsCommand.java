package com.gennlife.fs.configurations.patientdetail.conversion;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gennlife.darren.controlflow.for_.ForeachJSON.foreachKeyPath;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@_CommandName("array to grouped maps")
public class _ArrayToGroupedMapsCommand extends _Command {

    KeyPath fromGroup;
    KeyPath toGroup;
    KeyPath itemPath;
    KeyPath groupByPath;
    List<_From> froms;
    Map<String, _To> tos;
    List<_Command> commands;

    static _Command compile(JSONObject args) {
        _ArrayToGroupedMapsCommand ret = new _ArrayToGroupedMapsCommand();
        ret.fromGroup = KeyPath.compile(args.getString("from"));
        ret.toGroup = KeyPath.compile(args.getString("to"));
        ret.itemPath = KeyPath.compile(args.getString("item"));
        ret.groupByPath = KeyPath.compile(args.getString("groupBy"));
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
        ret.commands = _Command.compile(args.getJSONArray("commands"));
        return ret;
    }

    @Override
    boolean run(JSONObject source, JSONObject target) throws Exception {
        final AtomicBoolean ret = new AtomicBoolean(false);
        foreachKeyPath(source, fromGroup, (path, sourceArray) -> {
            if (sourceArray instanceof JSONArray) {
                final KeyPath targetArrayPath = path
                    .keyPathByRemovingLastUntilStringRemains(toGroup.size(), true)
                    .keyPathByReplacingStrings(toGroup);
                Map<Object, Integer> indexes = new HashMap<>();
                // noinspection unchecked
                for (JSONObject sourceObject: (List<JSONObject>)sourceArray) {
                    final Object sn = groupByPath.tryResolve(sourceObject);
                    // noinspection SuspiciousMethodCalls
                    final _To to = tos.get(itemPath.tryResolve(sourceObject));  // null if failed to resolve value
                    if (to != null) {
                        ret.set(true);
                        if (!targetArrayPath.isEmpty()) {
                            targetArrayPath.assignIfAbsent(target, new JSONArray());
                        }
                        final JSONArray targetArray = targetArrayPath.resolveAsJSONArray(target);
                        for (_From from: froms) {
                            final Object value = _TypeConversionPlugin.convert(from.field.tryResolve(sourceObject), from.type, to.type);
                            if (value != null) {
                                final JSONObject targetObject;
                                if (sn == null) {
                                    targetObject = new JSONObject();
                                    targetArray.add(targetObject);
                                } else {
                                    targetObject = targetArray.getJSONObject(
                                        indexes.computeIfAbsent(sn, k -> {
                                            targetArray.add(new JSONObject());
                                            return targetArray.size() - 1;
                                        }));
                                }
                                targetObject.put(to.field, value);
                                for (_Command command: commands) {
                                    command.run(sourceObject, targetObject);
                                }
                                break;
                            }
                        }
                    }
                }
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
