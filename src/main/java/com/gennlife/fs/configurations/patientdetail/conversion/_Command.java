package com.gennlife.fs.configurations.patientdetail.conversion;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.fs.configurations.GeneralConfiguration;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

import static com.gennlife.darren.controlflow.exception.Force.force;
import static com.gennlife.darren.controlflow.exception.Force.forcible;
import static com.gennlife.darren.controlflow.for_.Foreach.foreach;

class _Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralConfiguration.class);

    static List<_Command> compile(JSONArray objects) {
        List<_Command> ret = new ArrayList<>();
        forcible(() -> force(() -> {
            for (Object object: objects) {
                JSONObject o = (JSONObject)object;
                Method m = _NODE_TYPES.get(o.getString("_type"))
                    .getDeclaredMethod("compile", JSONObject.class);
                m.setAccessible(true);
                ret.add((_Command)m.invoke(null, o));
            }
        }));
        return ret;
    }

    boolean run(JSONObject source, JSONObject target) throws Exception {
        throw new Exception("Unimplemented node: " + nodeName());
    }

    final String nodeName() {
        return getClass().getAnnotation(_CommandName.class).value();
    }

    JSONObject configObject() {
        return new JSONObject().fluentPut("_type", nodeName());
    }

    @Override
    public String toString() {
        return configObject().toJSONString();
    }

    static Map<String, Class<? extends _Command>> _NODE_TYPES = force(() -> {
        HashMap<String, Class<? extends _Command>> ret = new HashMap<>();
        Reflections reflections = new Reflections(_Command.class.getPackage().getName());
        Set<Class<? extends _Command>> classes = reflections.getSubTypesOf(_Command.class);
        foreach(classes, clazz -> {
            _CommandName annotation = clazz.getAnnotation(_CommandName.class);
            if (annotation == null) {
                return;
            }
            String tag = annotation.value();
            if (ret.containsKey(tag)) {
                throw new Error("Conflict found: <" + tag + ">"
                    + " Classes: [" + ret.get(tag).getSimpleName() + "]"
                    + " <=> [" + clazz.getSimpleName() + "]");
            }
            ret.put(tag, clazz);
        });
        LOGGER.info("Loaded " + ret.size() + " node(s): " + ret.keySet());
        return ret;
    });

}
