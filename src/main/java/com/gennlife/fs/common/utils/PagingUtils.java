package com.gennlife.fs.common.utils;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author
 * @create 2019 22 18:26
 * @desc sd的分页工具
 **/
public class PagingUtils {

    public static List<JsonObject> getPageContentByApi(List<JsonObject> list, int pageNo, int pageSize){
        //总记录数
        int total = list.size();
        // 开始索引
        int fromIndex = (pageNo-1) * pageSize;
        // 结束索引
        int toIndex = fromIndex + pageSize;
        // 如果结束索引大于集合的最大索引，那么规定结束索引=集合大小
        if(toIndex > total){
            toIndex = total;
        }
        if(fromIndex <= total){
            List<JsonObject> subList = list.subList(fromIndex, toIndex);
            return subList;
        }else {
            return new ArrayList<>();
        }
    }

}
