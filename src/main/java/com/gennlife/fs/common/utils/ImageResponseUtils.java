package com.gennlife.fs.common.utils;

import com.gennlife.fs.common.response.ResponseInterface;
import com.gennlife.fs.service.patientsdetail.model.ImageResponse;
import com.gennlife.fs.service.patientsdetail.model.VisitSNResponse;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.gennlife.fs.system.bean.Compatible;

/**
 * Created by Chenjinfeng on 2017/8/18.
 */
public class ImageResponseUtils {
    private static final Compatible compatible = BeansContextUtil.getCompatible();

    public static ResponseInterface getImageResponseInterface(String[] key, String[] resultkeys) {
        ResponseInterface result = null;
        if (compatible == null || compatible.isDefaultImageGetFun()) {
            ImageResponse template = new ImageResponse(new VisitSNResponse(key, resultkeys));
            template.setimagekeys(resultkeys);
            result = template;
        }
        return result;
    }

    public static ResponseInterface getImageResponseInterface(String... key) {
        return getImageResponseInterface(key, key);
    }
}
