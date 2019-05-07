/**
 * copyRight
 */
package com.gennlife.fs.common.utils;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author liuzhen
 * Created by liuzhen.
 * Date: 2018/6/23
 * Time: 11:51
 */
@Component
public class HttpClient {
    private static Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);
//    private static int timeOut = 30000;
    @Autowired
    private HttpConnectionManager connectionManager;
    private static void config(HttpRequestBase httpRequestBase) {
        // 设置Header等
        httpRequestBase.setHeader("User-Agent", "Mozilla/5.0");
        httpRequestBase.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            httpRequestBase.setHeader("Accept-Language",
            "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
            httpRequestBase.setHeader("Accept-Charset",
            "ISO-8859-1,utf-8,gbk,gb2312;q=0.7,*;q=0.7");
    }

    public static void setTimeOut(HttpRequestBase httpRequestBase, int timeOut){
        // 配置请求的超时设置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeOut)
                .setConnectTimeout(timeOut)
                .setSocketTimeout(timeOut).build();
        httpRequestBase.setConfig(requestConfig);
    }

    public String executPost(String url, Map<String, Object> params, int timeOut) throws Exception{
        HttpPost httppost = new HttpPost(url);
        config(httppost);
        setTimeOut(httppost, timeOut);
        setPostParams(httppost, params);
        CloseableHttpResponse response = null;
        try {
            response = connectionManager.createHttpClient().execute(httppost,
                    HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "utf-8");
            //关闭entity 流
            EntityUtils.consume(entity);
            return result;
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                throw e;
            }
        }
    }
    public String executPostWithBody(String url, String params, int timeOut) throws Exception{
        HttpPost httppost = new HttpPost(url);
        config(httppost);
        setTimeOut(httppost,timeOut);
        StringEntity postParamsWithBody = getPostParamsWithBody(params);
        httppost.setEntity(postParamsWithBody);
        CloseableHttpResponse response = null;
        try {
            response = connectionManager.createHttpClient().execute(httppost,
                    HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "utf-8");
            //关闭entity 流
            EntityUtils.consume(entity);
            return result;
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                throw e;
            }
        }
    }
    private static void setPostParams(HttpPost httpost, Map<String, Object> params) {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        Set<String> keySet = params.keySet();
        for (String key : keySet) {
            nvps.add(new BasicNameValuePair(key, params.get(key).toString()));
        }

        try {
            httpost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("httpost.setEntity 异常，异常信息:{}",e);
        }
    }
    private static StringEntity getPostParamsWithBody(String params) {
        StringEntity entity = new StringEntity(params, "utf-8");
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        return entity;
    }
    public String executeGet(String url, int timeOut) throws Exception{
        HttpGet httpget = new HttpGet(url);
        config(httpget);
        setTimeOut(httpget, timeOut);
        CloseableHttpResponse response = null;
        try {
            response = connectionManager.createHttpClient().execute(httpget,
                    HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
            return result;
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                throw e;
            }
        }
    }
}
