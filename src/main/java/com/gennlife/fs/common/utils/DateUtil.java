package com.gennlife.fs.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Chenjinfeng on 2016/10/13.
 */
public class DateUtil {
    public static long getDurationWithDays(Date start,Date end)
    {
        return (long) Math.ceil((end.getTime() - start.getTime()) / (1000.0 * 60 * 60 * 24));
    }
    public static int date_compare(String d1,Date d2)
    {
        return getDate_ymd(d1).compareTo(d2);
    }
    public static int date_compare_ymd(String d1,String d2)
    {
        return getDate_ymd(d1).compareTo(getDate_ymd(d2));
    }
    public static boolean isInPeriod(String target,Date start,Date end)
    {
        if(StringUtil.isEmptyStr(target)) return false;
        long tmp = getDate_ymd(target).getTime();
        if(end==null) return tmp>=start.getTime();
        return (tmp>=start.getTime() && tmp<=end.getTime());

    }

    public static String formatDayTime(String time){
        Date date = getDate_ymdhms(time);
        return getDateStr_ymd(date);
    }

    public static long getDurationWithDays(String start,String end)
    {
        long days=0;
        try {
            days= (long) Math.ceil((getDate_ymd(start).getTime() - getDate_ymd(end).getTime())/(1000.0*60*60*24));
        }
        catch (Exception e)
        {
        }
        return days;

    }
    public static String getDateStr_ymd(Date date)
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(date);
    }
    public static String getDateStrByYmdHms(Date date)
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }
    public static Date getDate_ymd(String datestr) {
        if(StringUtil.isEmptyStr(datestr))return null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            return format.parse(datestr);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    public static Date getDate_ymdhms(String datestr) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return format.parse(datestr);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static String formatDateFromDate(Date date) {
        SimpleDateFormat dateFormat_hms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return dateFormat_hms.format(date);
        } catch (Exception localException) {
            try {
                return dateFormat.format(date) + "00:00:00";
            } catch (Exception localException1) {
            }
        }
        return dateFormat_hms.format(new Date());
    }

    public static Date getDate(String date) {
        SimpleDateFormat dateFormat_hms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateFormatYear = new SimpleDateFormat("yyyy");
        Date value=null;
        try {
           value = dateFormat_hms.parse(date);
            return value;
        //	return formatDateFromDate(value);
        } catch (Exception localException) {
            try {
                value = dateFormat.parse(date);
                return value;
            //	return formatDateFromDate(value);
            } catch (Exception localException1) {

                try {
                    value = dateFormatYear.parse(date);
                    return value;
                //	return formatDateFromDate(value);

                } catch (ParseException e) {
                    //e.printStackTrace();
                }
            }
        }
        return value;
    }

    public static boolean isDate(Object object) {

        if (object instanceof Date ) {
            return true;
        }

        String rexp = "([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})|(^((\\d{2}(([02468][048])|([13579][26]))[\\-\\/\\s]?((((0?[13578])|(1[02]))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(3[01])))|(((0?[469])|(11))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(30)))|(0?2[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])))))|(\\d{2}(([02468][1235679])|([13579][01345789]))[\\-\\/\\s]?((((0?[13578])|(1[02]))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(3[01])))|(((0?[469])|(11))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(30)))|(0?2[\\-\\/\\s]?((0?[1-9])|(1[0-9])|(2[0-8])))))))";

        Pattern pat = Pattern.compile(rexp);

        Matcher mat = pat.matcher(object.toString());

        boolean dateType = mat.matches();

        return dateType;
    }
    public static long StrToDays(String date)
    {
        return DateUtil.getDate_ymd(date).getTime()/(24*60*60*1000);
    }

    /**
     *
     * */
    public static String getNowDataYMD()
    {
        return getDateStr_ymd(new Date(System.currentTimeMillis()));
    }

    public static Integer getHouer(String examTime) {
        Date date = getDate(examTime);
        return date.getHours();
    }
}
