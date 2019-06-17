package com.gennlife.fs.common.utils;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.gennlife.fs.system.config.StatisticsSearch;
import com.gennlife.fs.system.config.TimeConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class HttpRequestUtils {
    private static Logger logger = LoggerFactory.getLogger(HttpRequestUtils.class);

    /**
     * post
     *
     * @param url
     * @param jsonParam
     * @return
     */
    public static String httpPost(String url, String jsonParam) {
        long s = System.currentTimeMillis();
        HttpClient httpClient = HttpRequestUtil.getConnection();
        HttpPost method = new HttpPost(url);
        String str = null;
        try {
            if (null != jsonParam) {
                setEntry(jsonParam, method);
            }
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(TimeConfig.getHTTPTIMEOUT())
                    .setConnectTimeout(TimeConfig.getHTTPTIMEOUT()).build();
            method.setConfig(requestConfig);
            method.setHeader("Accept-Encoding", "gzip,deflate");
            HttpResponse result = httpClient.execute(method);
            url = URLDecoder.decode(url, "UTF-8");
            if (result.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                //最大2G数据
                return getDataString(result);
            } else {
                logger.error("code [" + result.getStatusLine().getStatusCode() + "] get data ERROR url " + url + " jsonparam => " + jsonParam + " data =>" + EntityUtils.toString(result.getEntity()));
            }
        } catch (Exception e) {
            logger.error("" + url, e);
        } finally {
            if (httpClient != null) {
                httpClient = null;
            }
        }
        return str;
    }
    public static String getStatistics(String patSn){
        String searchUrl = BeansContextUtil.getUrlBean().getSearchServiceStatisticsUri();
//        String searchUrl = "http://10.0.5.91:8901/search-server";
        String url = searchUrl+"/statistics";
        StatisticsSearch statisticsSearch = new StatisticsSearch(patSn);
        String result = httpPost(searchUrl,JsonAttrUtil.toJsonStr(statisticsSearch));
        if("{}".equals(result)){
            try {
                logger.warn("patsn: "+patSn + "没有查到数据 重新查找");
                Thread.sleep(1000);
                result = httpPost(searchUrl,JsonAttrUtil.toJsonStr(statisticsSearch));
            }catch (Exception e){
                logger.error("没有查到数据发生了问题");
            }
        }
        return result;
    }
    public static String getStatistics(String patSn,boolean isASC){
        String searchUrl = BeansContextUtil.getUrlBean().getSearchServiceStatisticsUri();
//        String searchUrl = "http://10.0.5.91:8901/search-server";
        String url = searchUrl+"/statistics";
        StatisticsSearch statisticsSearch = new StatisticsSearch(patSn,isASC);
        String result = httpPost(searchUrl,JsonAttrUtil.toJsonStr(statisticsSearch));
        if("{}".equals(result)){
            try {
                logger.warn("patsn: "+patSn + "没有查到数据 重新查找");
                Thread.sleep(1000);
                result = httpPost(searchUrl,JsonAttrUtil.toJsonStr(statisticsSearch));
            }catch (Exception e){
                logger.error("没有查到数据发生了问题");
            }
        }
        return result;
    }

    public static QueryResult search(String url, JsonObject param) {
        return SearchTransposeUtils.getSearchResult(searchNoTrans(url, param));
    }

    public static QueryResult search(QueryParam param) {
        return SearchTransposeUtils.getSearchResult(searchNoTrans(BeansContextUtil.getUrlBean().getSearch_service_uri(), param));
    }

    public static QueryResult export(QueryParam param) {
        return SearchTransposeUtils.getSearchResult(exportNoTrans(param));
    }

    public static QueryResult search(String url, QueryParam param) {
        return SearchTransposeUtils.getSearchResult(searchNoTrans(url, param));
    }

    public static QueryResult searchNoTrans(String url, JsonObject param) {
        return new QueryResult(httpJsonPost(url, param.toString()));
    }

    public static QueryResult searchNoTrans(QueryParam param) {
        return new QueryResult(httpJsonPost(BeansContextUtil.getUrlBean().getSearch_service_uri(), param.getJson().toString()));
    }

    public static QueryResult exportNoTrans(QueryParam param) {
        return new QueryResult(httpJsonPost(BeansContextUtil.getUrlBean().getExportUri(), param.getJson().toString()));
    }

    public static QueryResult searchNoTrans(String url, QueryParam param) {
        return new QueryResult(httpJsonPost(url, param.getJson().toString()));
    }

    public static String httpGet(String url) {
        HttpClient httpClient =HttpRequestUtil.getConnection();
        HttpGet method = new HttpGet(url);
        method.setHeader("Accept-Encoding", "gzip,deflate");
        try {
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(TimeConfig.getHTTPTIMEOUT()).setConnectTimeout(TimeConfig.getHTTPTIMEOUT()).build();
            method.setConfig(requestConfig);
            HttpResponse result = httpClient.execute(method);
            if (result.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                try {
                    return getDataString(result);
                } catch (Exception e) {
                    logger.error("" + url, e);
                }
            } else {
                logger.error("code [" + result.getStatusLine().getStatusCode() + "] GET data error url " + url);
            }
        } catch (IOException e) {
            logger.error("" + url, e);
        }
        return null;
    }

    private static String getDataString(HttpResponse result) throws IOException {
        String str;
        Header contentHeader = result.getFirstHeader("Content-Encoding");
        if (contentHeader != null
                && contentHeader.getValue().toLowerCase().indexOf("gzip") > -1) {
            str = EntityUtils.toString(new GzipDecompressingEntity(result.getEntity()));
            return str;
        }
        str = EntityUtils.toString(result.getEntity(), "utf-8");
        return str;
    }

    public static JsonElement httpGetJson(String url) {
        return JsonAttrUtil.toJsonElement(httpGet(url));
    }

    public static JsonElement httpJsonPost(String url, String jsonParam) {
        if (StringUtil.isEmptyStr(jsonParam) || QueryParam.checkParamIsError(jsonParam)) {
            return null;
        }
        return JsonAttrUtil.toJsonElement(httpPost(url, jsonParam));
    }

    protected static void setEntry(String jsonParam, HttpPost method) throws UnsupportedEncodingException {
        StringEntity entity = null;
        entity = new StringEntity(jsonParam, "utf-8");
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        method.setEntity(entity);
    }

    public static QueryResult search(String url, String param) {
        return new QueryResult(httpJsonPost(url, param));
    }

    public static String getSearchEmr(String param) {
        String searchUrl = BeansContextUtil.getUrlBean().getSearchServicedetailsUri();
//        String searchUrl = "http://10.0.5.91:8901/search-server";
//        String url = searchUrl +"/emr/details";
        String result = httpPost(searchUrl,param);
        if("{}".equals(result)){
            try {
                logger.warn("param: "+param + "没有查到数据 重新查找");
                Thread.sleep(1000);
                result = httpPost(searchUrl,param);
            }catch (Exception e){
                logger.error("没有查到数据发生了问题");
            }
        }
        return result;
    }
}
