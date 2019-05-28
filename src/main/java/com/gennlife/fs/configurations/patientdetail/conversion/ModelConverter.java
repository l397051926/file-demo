package com.gennlife.fs.configurations.patientdetail.conversion;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

public class ModelConverter {

    public ModelConverter(JSONArray config) {
        _commands = _Command.compile(config);
    }

    public JSONObject convert(JSONObject data) throws Exception {
        if (data == null) {
            return null;
        }
        JSONObject ret = new JSONObject();
        for (_Command command: _commands) {
            command.run(data, ret);
        }
        return ret;
    }

    List<_Command> _commands = null;

}
