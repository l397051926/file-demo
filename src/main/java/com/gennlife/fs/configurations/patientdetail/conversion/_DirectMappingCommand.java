package com.gennlife.fs.configurations.patientdetail.conversion;

import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.gennlife.darren.controlflow.for_.ForeachJSON.foreachKeyPath;

@_CommandName("direct")
public class _DirectMappingCommand extends _Command {

    KeyPath from = null;
    KeyPath to = null;
    _TypeConversionPlugin cvt = null;

    static _Command compile(JSONObject args) {
        _DirectMappingCommand ret = new _DirectMappingCommand();
        ret.from = KeyPath.compile(args.getString("from"));
        ret.to = KeyPath.compile(args.getString("to"));
        JSONObject cvtArgs = args.getJSONObject("convert");
        if (cvtArgs != null) {
            ret.cvt = _TypeConversionPlugin.compile(cvtArgs);
        }
        return ret;
    }

    @Override
    boolean run(JSONObject source, JSONObject target) throws Exception {
        AtomicBoolean ret = new AtomicBoolean(false);
        foreachKeyPath(source, from, (path, value) -> {
            ret.set(true);
            if (cvt != null) {
                value = cvt.run(value);
            }
            if (value != null) {
                path.keyPathByReplacingStrings(to)
                    .keyPathByRemovingLastUntilStringRemains(to.size())
                    .assign(target, value);
            }
        });
        return ret.get();
    }

}
