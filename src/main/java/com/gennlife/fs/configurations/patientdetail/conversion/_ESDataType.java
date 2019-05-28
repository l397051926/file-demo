package com.gennlife.fs.configurations.patientdetail.conversion;

import java.util.HashMap;
import java.util.Map;

import static com.gennlife.darren.controlflow.exception.Force.force;

enum _ESDataType {

    TEXT,
    KEYWORD,
    DATE,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN;

    _EMRDataType toEMRType() {
        return TYPES.get(this);
    }

    private static Map<_ESDataType, _EMRDataType> TYPES = force(() -> {
        Map<_ESDataType, _EMRDataType> ret = new HashMap<>();
        ret.put(TEXT, _EMRDataType.STRING);
        ret.put(KEYWORD, _EMRDataType.STRING);
        ret.put(DATE, _EMRDataType.DATE);
        ret.put(INT, _EMRDataType.LONG);
        ret.put(LONG, _EMRDataType.LONG);
        ret.put(FLOAT, _EMRDataType.DOUBLE);
        ret.put(DOUBLE, _EMRDataType.DOUBLE);
        ret.put(BOOLEAN, _EMRDataType.BOOLEAN);
        return ret;
    });

}
