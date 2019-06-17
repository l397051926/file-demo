package com.gennlife.fs.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.collection.keypath.KeyPath;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.gennlife.fs.common.exception.FormatCorruptedException;
import com.gennlife.fs.common.exception.NotFoundException;
import com.gennlife.fs.common.exception.ResponseException;
import com.gennlife.fs.common.exception.TransferFailedException;
import lombok.Builder;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.gennlife.darren.controlflow.exception.Try.try_;
import static com.gennlife.fs.common.utils.HttpRequestUtil.postData;
import static com.gennlife.fs.common.utils.KeyPathUtil.toKeyPath;
import static java.util.stream.Collectors.*;

@Service
public class ProjectService {

    @Builder
    public static class BasicInfoParameters {
        String projectId;
    }

    @Builder
    public static class ProjectInfo {
        public int rawId;
        public String id;
        public String name;
        public String crfId;
        public String type;
        public String typeId;
        public String description;
        @Builder.Default
        public boolean deleted = false;
        public String creator;
        public String creatorId;
        public String creatorTitle;
        public String cooperator;
        public String cooperatorId;
        public String cooperatorTitle;
        public Long createTime;
        public Long modifyTime;
        public Long startTime;
        public Long finishTime;
        @Builder.Default
        List<CustomVariable> customVariables = new ArrayList<>();
        @Builder
        public static class CustomVariable {
            public String name;
            public String id;
            public JSONObject toJSONObject() {
                return new JSONObject()
                    .fluentPut("id", id)
                    .fluentPut("name", name);
            }
            public static CustomVariable fromJSONObject(JSONObject o) {
                return builder()
                    .id(o.getString("id"))
                    .name(o.getString("name"))
                    .build();
            }
            @Override
            public String toString() {
                return toJSONObject().toString();
            }
            public static CustomVariable fromString(String s) {
                return fromJSONObject(JSON.parseObject(s));
            }
        }
    }

    public ProjectInfo info(BasicInfoParameters params) throws ResponseException {
        val response = try_(() ->
            postData(
                cfg.rwsServiceEndpoint,
                cfg.rwsServiceProjectBasicInfoApi,
                new JSONObject()
                    .fluentPut("projectId", new JSONArray()
                        .fluentAdd(params.projectId))))
            .orElse(null);
        if (response == null) {
            throw new TransferFailedException();
        }
        val x = try_(() -> JSON.parseObject(response)).orElse(null);
        if (x == null) {
            throw new FormatCorruptedException("Response is not a valid JSON object: " + response);
        }
        val o = try_(() -> FIRST_SEARCH_RESULT_PATH.resolve(x)).orElse(null);
        if (o == null) {
            throw new NotFoundException("Project " + params.projectId + " may not exist. Response: " + response);
        }
        return ProjectInfo.builder()
            .rawId(toKeyPath("id").tryResolveAsInteger(o))
            .id(toKeyPath("projectId").tryResolveAsString(o))
            .name(toKeyPath("projectName").tryResolveAsString(o))
            .crfId(toKeyPath("crfId").tryResolveAsString(o))
            .type(toKeyPath("scientificName").tryResolveAsString(o))
            .typeId(toKeyPath("scientificId").tryResolveAsString(o))
            .description(toKeyPath("projectdesc").tryResolveAsString(o))
            .deleted(toKeyPath("isDelete").tryResolveAsBooleanValue(o))
            .creator(toKeyPath("creatorName").tryResolveAsString(o))
            .creatorId(toKeyPath("creatorId").tryResolveAsString(o))
            .creatorTitle(toKeyPath("headName").tryResolveAsString(o))
            .cooperator(toKeyPath("cooperIs").tryResolveAsString(o))
            .cooperatorId(null)
            .cooperatorTitle(toKeyPath("cooperHeadName").tryResolveAsString(o))
            .createTime(toKeyPath("creatorTime").tryResolveAsLong(o))
            .modifyTime(toKeyPath("modifyTime").tryResolveAsLong(o))
            .startTime(toKeyPath("startTime").tryResolveAsLong(o))
            .finishTime(toKeyPath("endTime").tryResolveAsLong(o))
            .customVariables(try_(
                () -> toKeyPath("activeIndices").resolveAsJSONArray(o)
                    .stream()
                    .map(JSONObject.class::cast)
                    .map(obj -> ProjectInfo.CustomVariable.builder()
                        .id(toKeyPath("id").resolveAsString(obj))
                        .name(toKeyPath("name").resolveAsString(obj))
                        .build())
                    .collect(toList()))
                .orElse(new ArrayList<>()))
            .build();
    }

    @Builder
    public static class PatientsParameters {
        String userId;
        String projectId;
    }

    // {"groupName": ["patientSn"]}
    public Map<String, List<String>> patients(PatientsParameters params) {
        val response = try_(() ->
            postData(
                cfg.rwsServiceEndpoint,
                cfg.rwsServiceProjectPatientsApi,
                new JSONObject()
                    .fluentPut("uid", params.userId)
                    .fluentPut("projectId", params.projectId)))
            .orElse(null);
        if (response == null) {
            throw new TransferFailedException();
        }
        val x = try_(() -> JSON.parseObject(response)).orElse(null);
        if (x == null) {
            throw new FormatCorruptedException("Response is not a valid JSON object: " + response);
        }
        val ret = try_(
            () -> SEARCH_RESULTS_PATH
                .resolveAsJSONObject(x)
                .entrySet()
                .stream()
                .collect(toMap(
                    Map.Entry::getKey,
                    e -> ((JSONArray)e.getValue())
                        .stream()
                        .map(String.class::cast)
                        .collect(toList()))))
            .orElse(null);
        if (ret == null) {
            throw new NotFoundException("Project " + params.projectId + " may not exist. Response: " + response);
        }
        return ret;
    }

    @Builder
    public static class ComputeCustomVariablesValueParameters {
        String userId;
        Long taskId;
        String projectId;
        String crfId;
        Collection<String> varIds;
        String patientSn;
    }

    public JSONObject computeCustomVariablesValue(ComputeCustomVariablesValueParameters params) {
        val response = try_(
            () ->
                postData(
                    cfg.rwsServiceEndpoint,
                    cfg.rwsServiceProjectComputeCustomVariableApi,
                    new JSONObject()
                        .fluentPut("uid", params.userId)
                        .fluentPut("taskId", params.taskId)
                        .fluentPut("projectId", params.projectId)
                        .fluentPut("crfId", params.crfId)
                        .fluentPut("calculations", params.varIds
                            .stream()
                            .collect(toCollection(JSONArray::new)))
                        .fluentPut("patientSns", new JSONArray().fluentAdd(params.patientSn))))
            .orElse(null);
        if (response == null) {
            throw new TransferFailedException();
        }
        val x = try_(() -> JSON.parseObject(response)).orElse(null);
        if (x == null) {
            throw new FormatCorruptedException("Response is not a valid JSON object: " + response);
        }
        return CUSTOM_VAR_VALUE_PATH.resolveAsJSONObject(x);
    }

    private static final KeyPath SEARCH_RESULTS_PATH = toKeyPath("data");
    private static final KeyPath FIRST_SEARCH_RESULT_PATH = SEARCH_RESULTS_PATH.keyPathByAppending(0);
    private static final KeyPath CUSTOM_VAR_VALUE_PATH = toKeyPath("data");

    @Autowired
    private GeneralConfiguration cfg;

}
