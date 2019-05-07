package com.gennlife.fs.common.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gennlife.fs.service.patientsdetail.model.Visit.TIME_EMPTY;

/**
 * Created by Chenjinfeng on 2016/10/20.
 */
public class StringUtil {
    private static final Logger logger = LoggerFactory.getLogger(StringUtil.class);

    public static boolean isInStrs(String findstr, String[] targets) {
        if (isEmptyStr(findstr) || targets == null) return false;
        for (String target : targets)
            if (findstr.equals(target))
                return true;
        return false;
    }

    public static boolean isEmptyStr(String str) {
        return (str == null || str.trim().equals(""));
    }

    public static boolean isContain(String from, String[] target) {
        if (isEmptyStr(from)) return false;
        if (target == null || target.length == 0) return false;
        for (String tarItem : target) {
            if (from.toLowerCase().contains(tarItem.toLowerCase())) return true;
        }
        return false;
    }

    public static String bytesToMD5(byte[] input) {
        String md5str = null;
        try {
            //创建一个提供信息摘要算法的对象，初始化为md5算法对象
            MessageDigest md = MessageDigest.getInstance("MD5");
            //计算后获得字节数组
            byte[] buff = md.digest(input);
            //把数组每一字节换成16进制连成md5字符串
            md5str = bytesToHex(buff);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return md5str;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer md5str = new StringBuffer();
        //把数组每一字节换成16进制连成md5字符串
        int digital;
        for (int i = 0; i < bytes.length; i++) {
            digital = bytes[i];
            if (digital < 0) {
                digital += 256;
            }
            if (digital < 16) {
                md5str.append("0");
            }
            md5str.append(Integer.toHexString(digital));
        }
        return md5str.toString().toUpperCase();
    }

    public static String getStringFromCollection(String join, Collection<String> collection) {
        if (collection == null || collection.size() == 0) return null;
        StringBuffer temp = new StringBuffer();
        for (String key : collection) {
            if (temp.length() == 0) temp.append(key);
            else {
                temp.append(join).append(key);
            }
        }
        return temp.toString();
    }

    public static String getNumberFromCollection(String join, Collection<Number> collection) {
        if (collection == null || collection.size() == 0) return null;
        String temp = null;
        for (Number key : collection) {
            if (isEmptyStr(temp)) temp = String.valueOf(key);
            else {
                temp = temp + join + key;
            }
        }
        return temp;
    }

    public static String getStringFromArray(String join, JsonArray list) {
        if (list == null || list.size() == 0) return null;
        String temp = null;
        String key = null;
        for (JsonElement item : list) {
            try {
                key = item.getAsString();
                if (isEmptyStr(temp)) temp = key;
                else {
                    temp = temp + join + key;
                }
            } catch (Exception e) {
            }
        }
        return temp;
    }

    public static String getJsonArrayString(Iterator<String> iter) {
        if (!iter.hasNext()) return null;
        StringBuffer result = new StringBuffer("[");
        String temp = null;
        String key = null;
        while (iter.hasNext()) {
            key = iter.next();
            if (isEmptyStr(temp)) temp = '\"' + key + '\"';
            else {
                temp = temp + ',' + '\"' + key + '\"';
            }
        }
        result.append(temp).append("]");
        return result.toString();
    }

    public static String MD5(String str) {
        try {
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算md5函数
            md.update(str.getBytes());
            // digest()最后确定返回md5 hash值，返回值为8为字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return bytesToMD5(str.getBytes());
        }
    }

    public static String findWithContain(String[] sources, String target) {
        if (target == null) return null;
        for (String source : sources) {
            if (target.contains(source) || source.contains(target)) return source;
        }
        return null;
    }

    public static boolean findWithEqual(String[] sources, String target) {
        if (target == null) return false;
        for (String source : sources) {
            if (target.equals(source)) return true;
        }
        return false;
    }

    public static boolean isNotEmptyStr(String str) {
        return !isEmptyStr(str);
    }

    public static String buildStrFromStrs(String join, String[] base, String ignore) {
        String temp = null;
        for (String key : base) {
            if (key.contains(ignore)) continue;
            if (isEmptyStr(temp)) temp = key;
            else {
                temp = temp + join + key;
            }
        }
        return temp;
    }

    public static String buildStrFromStrs(String join, String[] base) {
        String temp = null;
        for (String key : base) {
            if (isEmptyStr(temp)) temp = key;
            else {
                temp = temp + join + key;
            }
        }
        return temp;
    }

    public static String getSearchString(String model, String hasvalue, String nothasvalue) {
        String temp = null;
        if (!isEmptyStr(hasvalue) && !isEmptyStr(nothasvalue)) {
            temp = model + " 包含 " + hasvalue + " AND " + model + " 不包含 " + nothasvalue;
        } else if (!isEmptyStr(hasvalue)) {
            temp = model + " 包含 " + hasvalue;
        } else if (!isEmptyStr(nothasvalue)) {
            temp = model + " 不包含 " + nothasvalue;
        }
        return temp;
    }

    public static int[] getIntInterval(String value) {
        try {
            String[] size = value.split(",");
            int[] inter = new int[2];
            inter[0] = Integer.valueOf(size[0]);
            inter[1] = Integer.valueOf(size[1]);
            return inter;
        } catch (Exception e) {
            return null;
        }
    }

    public static Double[] getDoubleInterval(String value) {
        try {
            String[] size = value.split(",");
            Double[] inter = new Double[2];
            inter[0] = Double.valueOf(size[0]);
            inter[1] = Double.valueOf(size[1]);
            return inter;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean contain(String[] source, String target) {
        for (String temp : source) {
            if (temp.equals(target)) return true;
        }
        return false;
    }

    public static boolean isIV(String value) {
        if (StringUtil.isEmptyStr(value)) return false;
        return (value.contains("Ⅳ") || value.contains("IV"));
    }

    public static boolean isDateEmpty(String date) {
        if (isEmptyStr(date)) return true;
        if (date.equals(TIME_EMPTY)) return true;
        return false;
    }

    public static String get_visit_date(JsonObject visit_info_p) {
        String visit_date = "";
        JsonObject visit_info = visit_info_p;
        if (visit_info == null) return visit_date;
        if (visit_info.has("visit_info")) {
            visit_info = JsonAttrUtil.getJsonObjectValue("visit_info", visit_info_p);
            if (visit_info == null) {
                logger.warn("visit_info error");
                visit_info = visit_info_p;
            }
           /* logger.error("need visit_info json");
            return null;*/
        }
        if (visit_info.has("ADMISSION_DATE")) {
            visit_date = visit_info.get("ADMISSION_DATE").getAsString();
        }
        if (!StringUtil.isEmptyStr(visit_date)) return visit_date;
        if (visit_info.has("REGISTERED_DATE")) {
            visit_date = visit_info.get("REGISTERED_DATE").getAsString();
        } else {
            visit_date = "";
        }
        return visit_date;
    }

    public static String getDept(JsonObject visit_info) {
        String diagnosis_department = "";
        try {
            if (visit_info == null) return diagnosis_department;
            if (visit_info.has("REGISTERED_DEPT")) {
                diagnosis_department = visit_info.get("REGISTERED_DEPT").getAsString();
            } else if (visit_info.has("ADMISSION_DEPT")) {
                diagnosis_department = visit_info.get("ADMISSION_DEPT").getAsString();
            }
            return diagnosis_department;
        } catch (Exception e) {
            return diagnosis_department;
        }
    }

    /**
     * 抽取次数
     */
    public static String getTimesEvent(String item) {
        Pattern p = Pattern.compile("第(\\d+)次(.*)");
        Matcher m = p.matcher(item);
        if (m.find()) {
            return m.group(2);
        }
        return null;
    }

    public static String[] addIntoStringArray(String[] base, String... adds) {
        if (adds == null || adds.length == 0) return base;
        if (base == null || base.length == 0) return adds;
        String[] result = new String[base.length + adds.length];
        for (int i = 0; i < result.length; i++) {
            if (i < base.length)
                result[i] = base[i];
            else
                result[i] = adds[i - base.length];
        }
        return result;
    }
}
