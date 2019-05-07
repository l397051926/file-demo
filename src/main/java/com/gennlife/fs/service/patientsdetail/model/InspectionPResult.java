package com.gennlife.fs.service.patientsdetail.model;

import java.util.LinkedList;

/**
 * Created by Chenjinfeng on 2018/3/19.
 */
public class InspectionPResult {
    private String pName;
    private LinkedList<Inner> sub;

    public boolean search(String key) {
        if (sub == null || sub.size() == 0) return false;
        LinkedList<Inner> matchSub = new LinkedList<>();
        for (Inner inner : sub) {
            if (inner.getName().contains(key)) matchSub.add(inner);
        }
        if (matchSub.size() == 0) return false;
        sub = matchSub;
        return true;
    }

    public static class Inner {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


    public String getpName() {
        return pName;
    }

    public void setpName(String pName) {
        this.pName = pName;
    }

    public LinkedList<Inner> getSub() {
        return sub;
    }

    public void setSub(LinkedList<Inner> sub) {
        this.sub = sub;
    }
}
