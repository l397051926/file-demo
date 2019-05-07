package com.gennlife.fs.common.comparator;

import java.util.Comparator;

public class ReverseStringComparator implements Comparator<String> {
	@Override
	public int compare(String lhs, String rhs) {
		return lhs.compareTo(rhs);
	}
}