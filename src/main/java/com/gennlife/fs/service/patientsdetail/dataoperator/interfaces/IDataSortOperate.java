package com.gennlife.fs.service.patientsdetail.dataoperator.interfaces;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.List;

/**
 * Created by Chenjinfeng on 2017/7/7.
 */
public interface IDataSortOperate {
    List<JsonElement> sort(JsonArray sources);
    String getSortKey();
}
