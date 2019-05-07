package com.gennlife.fs.common.comparator;

import java.util.Comparator;
import java.util.Map;

public class LongKeyComparator<V> implements Comparator<Map.Entry<Long,V>> {

    @Override
    public int compare(Map.Entry<Long, V> o1, Map.Entry<Long, V> o2) {
        return o1.getKey().compareTo(o2.getKey());
    }
}