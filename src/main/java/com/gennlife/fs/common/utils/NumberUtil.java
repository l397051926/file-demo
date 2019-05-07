package com.gennlife.fs.common.utils;

/**
 * Created by Chenjinfeng on 2016/12/30.
 */
public class NumberUtil {
	private final static String[] unit=new String[]{" B "," KB "," MB"," GB"};
    public static long ceil(double value)
    {
        return (long)Math.ceil(value);
    }
    public static double keepPoint(double value,long point)
    {
        long pointlong= (long) Math.pow(10,point);
        return (long)(value*pointlong+0.5)*1.0/pointlong;
    }

	public static String countSize(long size)
	{

		int count=0;
		double tmp=size*1.0;
		int max=unit.length-1;
		while(tmp>1024 && count<max)
		{
			tmp=tmp/1024.0;
			count++;
		}
		return (long)(tmp*1000)/1000.0+unit[count];

	}
}
