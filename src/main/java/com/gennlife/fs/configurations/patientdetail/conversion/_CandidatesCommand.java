package com.gennlife.fs.configurations.patientdetail.conversion;

import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.Pair;
import com.gennlife.darren.collection.keypath.KeyPath;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gennlife.darren.collection.Pair.makePair;
import static com.gennlife.darren.controlflow.for_.Foreach.foreach;
import static com.gennlife.darren.controlflow.for_.ForeachJSON.foreachKeyPath;
import static com.gennlife.darren.controlflow.for_.ForeachJSON.foreachOuterMostLayerWithoutArray;

@_CommandName("candidates")
public class _CandidatesCommand extends _Command {

    KeyPath from = null;  // a.b
    KeyPath to = null;    // a.b.TARGET_FIELD
    List<Pair<KeyPath, _TypeConversionPlugin>> candidates = new ArrayList<>();
    // [(SOURCE_FIELD_0, cvt), (SOURCE_FIELD_1, cvt)]

    static _Command compile(JSONObject args) {
        _CandidatesCommand ret = new _CandidatesCommand();
        ret.from = KeyPath.compile(args.getString("from"));
        ret.to = KeyPath.compile(args.getString("to"));
        _EMRDataType fromType = _EMRDataType.valueOf(args.getString("type").toUpperCase());
        foreach(args.getJSONArray("candidates"), candidate -> {
            JSONObject o = (JSONObject)candidate;
            _EMRDataType toType = _EMRDataType.valueOf(o.getString("type").toUpperCase());
            ret.candidates.add(makePair(
                KeyPath.compile(o.getString("field")),
                new _TypeConversionPlugin(fromType, toType)));
        });
        return ret;
    }

    @Override
    boolean run(JSONObject source, JSONObject target) throws Exception {
        AtomicBoolean ret = new AtomicBoolean(false);
        foreachKeyPath(source, from, (pathA, arr) ->
            foreachOuterMostLayerWithoutArray(arr, (pathB, obj) -> {
                for (Pair<KeyPath, _TypeConversionPlugin> candidate: candidates) {
                    AtomicBoolean found = new AtomicBoolean(false);
                    foreachKeyPath(obj, candidate.key(), (pathC, value) -> {
                        if (value != null) {
                            ret.set(true);
                            found.set(true);
                            new KeyPath(pathA, pathB, pathC)
                                .keyPathByReplacingStrings(to)
                                .keyPathByRemovingLastUntilStringRemains(to.size())
                                .assign(target, candidate.value().run(value));
                        }
                    });
                    if (found.get()) {
                        break;
                    }
                }
            }));
        return ret.get();
    }

}
