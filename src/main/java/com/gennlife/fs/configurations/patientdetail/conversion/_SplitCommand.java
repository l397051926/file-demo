package com.gennlife.fs.configurations.patientdetail.conversion;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gennlife.darren.controlflow.for_.Foreach.foreach;
import static com.gennlife.darren.controlflow.for_.ForeachJSON.foreachKeyPath;

@_CommandName("split")
public class _SplitCommand extends _Command {

    KeyPath fromGroup = null;
    KeyPath field = null;
    Map<String, _Option> options = new HashMap<>();

    static _Command compile(JSONObject args) {
        _SplitCommand ret = new _SplitCommand();
        ret.fromGroup = KeyPath.compile(args.getString("group"));
        ret.field = KeyPath.compile(args.getString("field"));
        foreach(args.getJSONObject("options"), (key, value) -> {
            JSONObject sub = (JSONObject)value;
            _Option option = new _Option();
            option.toGroup = KeyPath.compile(sub.getString("group"));
            option.commands = _Command.compile(sub.getJSONArray("commands"));
            ret.options.put(key, option);
        });
        return ret;
    }

    @Override
    boolean run(JSONObject source, JSONObject target) throws Exception {
        AtomicBoolean ret = new AtomicBoolean(false);
        foreachKeyPath(source, fromGroup, (path, sourceArray) -> {
            if (sourceArray instanceof JSONArray) {
                // noinspection unchecked
                foreach((List<JSONObject>)sourceArray, sourceObject -> {
                    // noinspection SuspiciousMethodCalls
                    _Option option = options.get(field.tryResolve(sourceObject));  // null if failed to resolve value
                    if (option != null) {
                        ret.set(true);
                        JSONObject targetObject = new JSONObject();
                        for (_Command command: option.commands) {
                            command.run(sourceObject, targetObject);
                        }
                        KeyPath toPath = path
                            .keyPathByRemovingLastUntilStringRemains(option.toGroup.size())
                            .keyPathByReplacingStrings(option.toGroup);
                        toPath.assignIfAbsent(target, new JSONArray());
                        toPath.tryResolveAsJSONArray(target).add(targetObject);
                    }
                });
            }
        });
        return ret.get();
    }

    private static class _Option {
        KeyPath toGroup = null;
        List<_Command> commands = new ArrayList<>();
    }

}
