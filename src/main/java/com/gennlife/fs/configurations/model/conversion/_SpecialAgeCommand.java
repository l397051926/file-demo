package com.gennlife.fs.configurations.model.conversion;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.gennlife.darren.controlflow.for_.Foreach.foreachWithIndex;
import static com.gennlife.darren.controlflow.for_.ForeachJSON.foreachKeyPath;

@_CommandName("special (age)")
public class _SpecialAgeCommand extends _Command {

    KeyPath from;
    KeyPath to;

    // source
    KeyPath valuePath;
    KeyPath unitPath;

    // target
    Map<String, KeyPath> options = new HashMap<>();
    String defaultOption = null;

    static _Command compile(JSONObject args) {
        _SpecialAgeCommand ret = new _SpecialAgeCommand();
        ret.from = KeyPath.compile(args.getString("from"));
        ret.to = KeyPath.compile(args.getString("to"));
        ret.valuePath = KeyPath.compile(args.getString("value"));
        ret.unitPath = KeyPath.compile(args.getString("unit"));
        args.getJSONObject("options")
            .forEach((option, path) ->
                ret.options.put(option, KeyPath.compile((String)path)));
        ret.defaultOption = args.getString("default");
        return ret;
    }

    @Override
    boolean run(JSONObject source, JSONObject target) throws Exception {
        // noinspection unchecked
        foreachKeyPath(source, from, (path, group) -> {
            if (group instanceof JSONArray) {
                // noinspection unchecked
                foreachWithIndex((List<JSONObject>)group, (i, info) -> {
                    final Number value = (Number)valuePath.tryResolve(info);
                    if (value != null) {
                        // noinspection SuspiciousMethodCalls
                        path.keyPathByAppending(i)
                            .keyPathByReplacingStrings(to)
                            .keyPathByRemovingLastUntilStringRemains(to.size(), true)
                            .keyPathByAppending(Optional
                                .ofNullable(options.get(unitPath.tryResolve(info)))
                                .orElse(options.get(defaultOption)))
                            .assign(target, value);
                    }
                });
            }
        });
        return true;
    }

}
