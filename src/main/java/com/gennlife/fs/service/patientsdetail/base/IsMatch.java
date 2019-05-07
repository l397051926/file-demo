package com.gennlife.fs.service.patientsdetail.base;

import com.google.gson.JsonElement;

public interface IsMatch {
    boolean isMatch(JsonElement value);
}