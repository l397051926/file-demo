package com.gennlife.fs.common.enums;

/**
 * @author lmx
 * @create 2019 01 9:21
 * @desc  给三测单分区使用，若规则改变 ，在随之改变 目前是以区间判定
 **/
public enum  TripleTestPartitionEnum {
    ONE(0),TWO(1),THREE(2),FOUR(3),FIVE(4),SIX(5);

    private final Integer num;

    TripleTestPartitionEnum(Integer num) {
        this.num = num;
    }

    public static TripleTestPartitionEnum getEnumKey(int num, int hour){

        if( (hour > trans24hours(22,num) && hour > trans24hours(19,num))  || hour < trans24hours(3,num)  ){
            return ONE;
        }else if(hour >= trans24hours(3,num) && hour < trans24hours(7,num)){
            return TWO;
        }else if(hour >=trans24hours(7,num) && hour < trans24hours(11,num) ){
            return THREE;
        }else if(hour >=trans24hours(11,num) && hour < trans24hours(15,num) ){
            return FOUR;
        }else if(hour >= trans24hours(15,num) && hour < trans24hours(19,num) ){
            return FIVE;
        }else {
            return SIX;
        }
    }

    private static int trans24hours(int res,int num){
        return (res+num ) % 24;
    }

    public Integer getNum() {
        return num;
    }

    public static void main(String[] args) {
        System.out.println(TripleTestPartitionEnum.getEnumKey(0,2).getNum());
    }
}
