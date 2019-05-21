package com.gennlife.fs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.darren.collection.keypath.KeyPathSet;
import com.gennlife.fs.common.configurations.GeneralConfiguration;
import com.gennlife.fs.common.configurations.Model;
import com.gennlife.fs.common.exception.FormatCorruptedException;
import com.gennlife.fs.common.exception.TransferFailedException;
import com.gennlife.fs.common.utils.KeyPathUtil;
import lombok.Builder;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.gennlife.darren.controlflow.exception.Try.try_;
import static com.gennlife.fs.common.utils.HttpRequestUtil.postData;
import static com.gennlife.fs.common.utils.KeyPathUtil.toKeyPath;
import static com.gennlife.fs.common.utils.KeyPathUtil.toPathString;
import static java.util.stream.Collectors.toCollection;

@Service
public class SearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectExportTask.class);

    @Builder
    public static class FetchPatientDataParameters {
        Model model;
        String patientSn;
        KeyPathSet fields;
    }

    public JSONObject fetchPatientData(FetchPatientDataParameters params) {
        val groups = params.fields
            .stream()
            .map(KeyPath::keyPathByRemovingLast)
            .collect(toCollection(KeyPathSet::new));
        groups.removeAllDescendantsThatHasAncestor();
        groups.add(params.model.patientSnField());
        if (groups.contains(params.model.partitionGroup(), true)) {
            groups.add(params.model.partitionGroup().keyPathByAppending(params.model.partitionField()));
        }
        val body = new JSONObject()
            .fluentPut("indexName", params.model.indexName())
            .fluentPut("query", "[" + toPathString(params.model.fieldInfo(params.model.patientSnField()).displayPath) + "] 包含 " + params.patientSn)
            .fluentPut("size", 1)
            .fluentPut("page", 1)
            .fluentPut("needHighlight", false)
            .fluentPut("source", groups
                .stream()
                .map(KeyPathUtil::toPathString)
                .collect(toCollection(JSONArray::new)));
        val response = try_(() -> postData(cfg.searchServerEndpoint, cfg.searchServerSearchApi, body)).orElse(null);
        if (response == null) {
            throw new TransferFailedException();
        }
        val x = try_(() -> JSON.parseObject(response)).orElse(null);
        if (x == null) {
            throw new FormatCorruptedException("Response is not a valid JSON object: " + response + ", request: " + body.toJSONString());
        }
        try {
            return PATIENT_DATA_PATH.resolveAsJSONObject(x);
        } catch (Exception e) {
//            LOGGER.warn("Patient " + params.patientSn + " may not exist. Response: " + response + ", request: " + body.toJSONString());
            return new JSONObject();
        }
    }

    private static final KeyPath PATIENT_DATA_PATH = toKeyPath("hits.hits[0]._source");

    @Autowired
    private GeneralConfiguration cfg;

}
