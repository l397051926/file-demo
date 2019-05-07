package com.gennlife.fs.common.comparator;

import java.util.Comparator;

public class StringComparator implements Comparator<String> {
	@Override
	public int compare(String lhs, String rhs) {
		return lhs.compareTo(rhs);
	}
}