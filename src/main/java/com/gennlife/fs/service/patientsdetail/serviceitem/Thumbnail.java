package com.gennlife.fs.service.patientsdetail.serviceitem;

import com.gennlife.fs.common.response.ResponseMsgFactory;
import com.gennlife.fs.common.utils.Dcm2JpgUtils;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.system.config.SmbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by Chenjinfeng on 2017/7/26.
 */
@ConditionalOnBean(SmbConfig.class)
@Component
public class Thumbnail {

    private static SmbConfig smbConfig;
    private static final Logger logger = LoggerFactory.getLogger(Thumbnail.class);

    @Autowired
    public void setSmbConfig(SmbConfig smbConfig) {
        Thumbnail.smbConfig = smbConfig;
    }

    public void getImage(String url, HttpServletResponse response) {
        if (StringUtil.isEmptyStr(url)) {
            return;
        }
        if (smbConfig == null) {
            logger.error("SMB 配置 不可用");
            return;
        }
        String path = smbConfig.getLocalpath();
        if (StringUtil.isEmptyStr(path)) {
            logger.error("SMB local path error ");
            return;
        }
        File responseFile = null;
        try {
            response.setContentType("image/jpg");
            File file = null;
            url = url.trim();
            if (!url.endsWith(".dcm")) url = url + ".dcm";
            StringBuffer buffer = new StringBuffer(path);
            path = path.replace("\\", "/");
            url = url.replace("\\", "/").replace("//", "/");
            if (url.startsWith("/"))
                url = url.substring(1);
            String filePath = path.endsWith("/") ? buffer.append(url).toString() : buffer.append("/").append(url).toString();
            logger.info("filepath : " + filePath);
            file = new File(filePath);
            if (!file.exists()) {
                logger.error("图片不存在");
                return;
            }
            Dcm2JpgUtils conv = new Dcm2JpgUtils();
            responseFile = File.createTempFile("smb2jpg", ".jpg");
            conv.convert(file, responseFile);
        } catch (Exception e) {
            logger.error("", e);
        }
        if (responseFile != null) {
            FileInputStream fi = null;
            try {
                fi = new FileInputStream(responseFile);
                ResponseMsgFactory.responseStream(fi, response);
            } catch (Exception e) {
                logger.error("", e);
            } finally {
                if (fi != null) try {
                    fi.close();
                } catch (IOException e) {

                }
            }
            responseFile.deleteOnExit();
        }
    }
}
