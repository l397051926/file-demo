package com.gennlife.fs.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.gennlife.darren.util.ImmutableEndpoint;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 
 * @Description HttpRequestUtil
 * @author 安西平
 * @version 1.0.0
 * @DateTime
 * 
 */

public class HttpRequestUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestUtil.class);

	private static PoolingHttpClientConnectionManager connectionManager = null;
	private static HttpClientBuilder httpBulder = null;
	private static RequestConfig requestConfig = null;
	private static int MAX_CONNECTION = 50;
	private static int DEFAULT_MAX_CONNECTION = 10;
	static {
		requestConfig = RequestConfig.custom().setSocketTimeout(5000).setConnectTimeout(5000)
				.setConnectionRequestTimeout(5000).build();
		connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(MAX_CONNECTION);
		connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTION);

		httpBulder = HttpClients.custom();
		httpBulder.setConnectionManager(connectionManager);
	}

	public static CloseableHttpClient getConnection() {
		CloseableHttpClient httpClient = httpBulder.build();
		// httpClient = httpBulder.build();
		return httpClient;
	}

	public static HttpUriRequest getRequestMethod(Map<String, String> map, String url, String method) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		Set<Map.Entry<String, String>> entrySet = map.entrySet();
		for (Map.Entry<String, String> e : entrySet) {
			String name = (String) e.getKey();
			String value = (String) e.getValue();
			NameValuePair pair = new BasicNameValuePair(name, value);
			params.add(pair);
		}
		HttpUriRequest reqMethod = null;
		if ("post".equals(method)) {
			reqMethod =

					RequestBuilder.post().setUri(url)
							.addParameters((NameValuePair[]) params.toArray(new BasicNameValuePair[params.size()]))
							.setConfig(requestConfig).build();
		} else if ("get".equals(method)) {
			reqMethod =

					RequestBuilder.get().setUri(url)
							.addParameters((NameValuePair[]) params.toArray(new BasicNameValuePair[params.size()]))
							.setConfig(requestConfig).build();
		}
		
		
		
		return reqMethod;
	}

	/**
	 * 从服务器获取指定的url的值
	 *
	 * @param client
	 *            服务器client
	 * @param url
	 *            url
	 * @param method
	 *            访问方法 "get" "post"
	 * @return
	 * 
	 */
	private static String requestServer(HttpClient client, String url, String method) {
		Map<String, String> map = new HashMap<String, String>();
		HttpUriRequest get = getRequestMethod(map, url, method);
		HttpResponse response = null;

		String value = null;
		try {
			response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == 200) {
				HttpEntity entity = response.getEntity();
				value = EntityUtils.toString(entity, "utf-8");
			}
		} catch (IOException e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.error("Failed to send " + method.toUpperCase() + " request to " + url);
			}
		}
		return value;
	}

	public static String post(String url) {
		HttpClient client = getConnection();
		return requestServer(client, url, "post");
	}

	public static String get(String url) {
		HttpClient client = getConnection();
		return requestServer(client, url, "get");
	}

	public static String get(ImmutableEndpoint ep, String path) {
		HttpClient client = getConnection();
		return requestServer(client, "http://" + ep.toString() + path, "get");
	}

	/**
	 * 设置请求参数
	 * 
	 * @param
	 * @return
	 */
	private static List<NameValuePair> setHttpParams(Map<String, String> paramMap) {
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		Set<Map.Entry<String, String>> set = paramMap.entrySet();
		for (Map.Entry<String, String> entry : set) {
			formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
		return formparams;
	}

	public static String postData(ImmutableEndpoint ep, String path, Object param) {
		return postData(ep.host(), ep.port(), path, param);
	}

	public static String postData(String serverHost, int serverPort, String path, Object param) {
		return postData(serverHost, Integer.toString(serverPort), path, param);
	}
	
	public static String postData(String serverHost, String serverPort,String path, Object param) {
		
		StringBuffer buffer = new StringBuffer("http://");
		buffer.append(serverHost);
		buffer.append(":");
		buffer.append(serverPort);
		buffer.append(path);
		
        String apiURL = buffer.toString();
	    HttpClient httpClient = getConnection();  
		HttpPost method = new HttpPost(apiURL);  		     
        
        method.addHeader("Content-type","application/json; charset=utf-8");  
        method.setHeader("Accept", "application/json");  
        method.setEntity(new StringEntity(param.toString(), Charset.forName("UTF-8")));  
        
        HttpResponse response;
		try {
			response = httpClient.execute(method);
	        int statusCode = response.getStatusLine().getStatusCode();  
	        if (statusCode != 200) {  
	           return null;
	        }  
	        // Read the response body  
	        String body = EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));  
			return body;
		} catch (IOException e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.error("Failed to send POST request to " + apiURL + " with data: " + param);
			}
		}
		return null;
	}
	
	public static void main(String[] args) {

		HttpGet httpGet = new HttpGet("");

		httpGet.addHeader("Content-type", "application/json; charset=utf-8");
		httpGet.setHeader("Accept", "application/json");
		Header headers[] = httpGet.getAllHeaders();
		for (Header header : headers) {
			System.out.println(header.getName() + "  " + header.getValue());
		}
		
		JSONObject param = new JSONObject();
		param.put("text", "中华人民共和国MN");
		param.put("tokenizer", "ik_smart");

		String string = postData("10.0.5.51","9202","/hospital_clinical_patients/_analyze",param);		
		System.out.println(string);

	}
}
