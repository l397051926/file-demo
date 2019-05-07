package com.gennlife.fs.common.utils;
import com.gennlife.fs.system.config.SmbConfig;
import jcifs.smb.SmbFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Created by Chenjinfeng on 2017/7/26.
 */
public class SmbFileUtils {
    private static final Logger logger = LoggerFactory.getLogger(SmbFileUtils.class);

    public static SmbFile getSmbFile(String userName, String passwd, String url) throws IOException {
        if (!StringUtil.isEmptyStr(userName) && !StringUtil.isEmptyStr(passwd)) {
            StringBuffer buffer = new StringBuffer("smb://");
            buffer.append(userName).append(":").append(passwd).append("@");
            if (url.startsWith("\\\\")) buffer.append(url.substring("\\\\".length()));
            else buffer.append(url);
            url = buffer.toString();
        }
        url = url.replace("\\", "/");
        logger.info("smb url " + url);
        SmbFile smbfile = new SmbFile(url);
        return smbfile;
    }

    public static SmbFile getSmbFile(SmbConfig config, String url) throws IOException {
        if (config == null) return getSmbFile(null, null, url);
        return getSmbFile(config.getUserName(), config.getPasswd(), url);
    }

    public static void copy(File smbFile, File localFile) throws IOException {
        if (smbFile == null) {
            throw new IOException("共享文件不存在");
        }
        FileInputStream inputStream = new FileInputStream(smbFile);
        copy(inputStream, localFile);
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e) {
            }
        }

    }

    public static void copy(SmbFile smbFile, File localFile) throws IOException {
        if (smbFile == null) {
            throw new IOException("共享文件不存在");
        }
        copy(smbFile.getInputStream(), localFile);
    }

    public static void copy(InputStream smbFileInputStream, File localFile) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        if (smbFileInputStream == null) {
            throw new IOException("共享文件不存在");
        }
        try {
            in = new BufferedInputStream(smbFileInputStream);
            out = new BufferedOutputStream(new FileOutputStream(localFile));
            byte[] buffer = new byte[1024];
            while (in.read(buffer) != -1) {
                out.write(buffer);
                buffer = new byte[1024];
            }
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException e) {

            }
        }
    }
}
