package com.gennlife.fs.service.patientsdetail.model;

import java.util.Comparator;

/**
 * Created by Chenjinfeng on 2016/10/2.
 */
public class VisitComparatorASC implements Comparator<Visit> {
    /***
     * 时间升序
     */
    @Override
    public int compare(Visit o1, Visit o2) {
            return o1.get_visit_date().compareTo(o2.get_visit_date());

    }


}
