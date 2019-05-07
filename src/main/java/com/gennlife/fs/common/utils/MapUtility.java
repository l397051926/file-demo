package com.gennlife.fs.common.utils;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by nostalgia on 14-12-25.
 */
public class MapUtility {

	// 增加 Map 中某个 key 的计数
	public static <KEY> void increaseMapKeyCount(final Map<KEY, Long> map, KEY key, long count) {
		if (!map.containsKey(key)) {
			map.put(key, count);
		}
		else {
			map.put(key, map.get(key)+count);
		}
	}

	public static <KEY> void increaseMapKeyCount(final Map<KEY, Long> map, KEY key) {
		increaseMapKeyCount(map, key, 1);
	}

	// 合并两个 Map：对于相同的 key，将它们的 value 累加起来
	public static <KEY> void mergeMap(Map<KEY, Long> sum, final Map<KEY, Long> a, final Map<KEY, Long> b) {
		sum.clear();
		sum.putAll(a);
		for (Map.Entry<KEY, Long> entry : b.entrySet()) {
			increaseMapKeyCount(sum, entry.getKey(), entry.getValue());
		}
	}


	public static void main(String[] arg) {
		HashMap<Long, Long> a = new HashMap<Long, Long>();
		HashMap<Long, Long> b = new HashMap<Long, Long>();
		HashMap<Long, Long> sum = new HashMap<Long, Long>();

		MapUtility.increaseMapKeyCount(a, -1L, 1);
		MapUtility.increaseMapKeyCount(a, 11L, 1);
		MapUtility.increaseMapKeyCount(b, -1L, 1);
		MapUtility.increaseMapKeyCount(b, 22L, 1);
		MapUtility.mergeMap(sum, a, b);

		System.out.println("map a:");
		for (Map.Entry<Long, Long> entry : a.entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
		System.out.println();

		System.out.println("map b:");
		for (Map.Entry<Long, Long> entry : b.entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
		System.out.println();

		System.out.println("map sum:");
		for (Map.Entry<Long, Long> entry : sum.entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
		System.out.println();
	}
}
