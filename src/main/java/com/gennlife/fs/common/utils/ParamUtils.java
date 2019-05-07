package com.gennlife.fs.common.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;


/**
 * Created by chensong on 2015/12/9.
 */
public class ParamUtils {
    private static Logger logger = LoggerFactory.getLogger(ParamUtils.class);

    public static String getParam(HttpServletRequest request) {
        String param = null;
        if ("GET".equals(request.getMethod())) {
            param = request.getParameter("param");
        } else {
            param = getPostParm(request);
        }
        return cleanXSS(param);
    }

    private static String getPostParm(HttpServletRequest request) {
        StringBuffer jb = new StringBuffer();
        String line = null;
        BufferedReader reader = null;
        try {
            reader = request.getReader();
            if (!reader.ready()) return jb.toString();
            while ((line = reader.readLine()) != null)
                jb.append(line);
        } catch (IllegalStateException e){
            logger.error("getReader 被调用了两次的原因么" );
        } catch (Exception e) {
            logger.error("读取请求参数出错", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        }
        return jb.toString();
    }

    private static String cleanXSS(String value) {
        if (value == null) {
            return null;
        }
        value = value.replaceAll("eval", "");
        value = value.replaceAll("<script>", "");
        value = value.replaceAll("<javascript>", "");
        return value;
    }
}
