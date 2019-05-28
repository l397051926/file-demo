package com.gennlife.fs.configurations.patientdetail.conversion;

import com.alibaba.fastjson.JSONObject;

import static com.gennlife.fs.configurations.patientdetail.conversion._Converters.DEFAULT_CONVERTERS;

public class _TypeConversionPlugin {

    _EMRDataType from;
    _EMRDataType to;

    _TypeConversionPlugin(_EMRDataType from, _EMRDataType to) {
        this.from = from;
        this.to = to;
    }

    static _TypeConversionPlugin compile(JSONObject args) {
        return new _TypeConversionPlugin(
            _EMRDataType.valueOf(args.getString("from").toUpperCase()),
            _EMRDataType.valueOf(args.getString("to").toUpperCase())
        );
    }

    Comparable run(Object value) {
        return convert(value, from, to);
    }

    static Comparable convert(Object value, _EMRDataType from, _EMRDataType to) {
        return DEFAULT_CONVERTERS.get(to).apply(value);
    }

}
