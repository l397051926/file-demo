package com.gennlife.fs.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.controlflow.function.ThrowableConsumer;
import com.gennlife.darren.util.ImmutableEndpoint;
import com.gennlife.fs.configurations.GeneralConfiguration;
import com.gennlife.fs.common.exception.ResponseException;
import com.gennlife.fs.common.exception.UndefinedException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.alibaba.fastjson.serializer.SerializerFeature.WriteMapNullValue;
import static com.gennlife.darren.controlflow.exception.Try.try_;
import static com.gennlife.fs.common.utils.HttpRequestUtil.postData;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

public abstract class ControllerBase {

    private static Logger LOGGER = LoggerFactory.getLogger(ControllerBase.class);

    private static String serializeResponse(JSON json) {
        return toJSONString(json, WriteMapNullValue);
    }

    protected <T> Object dispatch(ImmutableEndpoint endpoint, String path, Object postBody, Function<T, Object> f, T parameter) {
        if (cfg.localEndpoint.equals(endpoint)) {
            return f.apply(parameter);
        } else {
            return try_(() -> JSON.parse(postData(endpoint, cfg.applicationContextPath + path, postBody)))
                .orElseGet(() -> f.apply(parameter));
        }
    }

    protected static String run(Supplier<Object> f) {
        return run(null, o -> f.get());
    }

    protected static String run(String request, Function<JSONObject, Object> f) {
        Object ret = null;
        try {
            ret = f.apply(request != null ? JSON.parseObject(request) : null);
        } catch (ResponseException e) {
            LOGGER.error(e.getLocalizedMessage() + ", request: " + request, e);
            ret = e.toJSONObject();
        } catch (Exception e) {
            LOGGER.error(e.getLocalizedMessage() + ", request: " + request, e);
            ret = new UndefinedException(e).toJSONObject();
        }
        if (ret instanceof JSON) {
            ret = serializeResponse((JSON)ret);
        }
        return ret.toString();
    }

    protected static void run(String request, HttpServletResponse response, ThrowableConsumer<JSONObject, ? extends Exception> f) throws IOException {
        ResponseException re = null;
        try {
            f.accept(JSON.parseObject(request));
        } catch (ResponseException e) {
            LOGGER.error(e.getLocalizedMessage() + ", request: " + request, e);
            re = e;
        } catch (Exception e) {
            LOGGER.error(e.getLocalizedMessage() + ", request: " + request, e);
            re = new UndefinedException(e);
        }
        if (re != null) {
            response.setContentType(APPLICATION_JSON_UTF8_VALUE);
            IOUtils.write(serializeResponse(re.toJSONObject()), response.getOutputStream(), StandardCharsets.UTF_8);
            response.flushBuffer();
        }
    }

    @Autowired
    protected GeneralConfiguration cfg;

}
