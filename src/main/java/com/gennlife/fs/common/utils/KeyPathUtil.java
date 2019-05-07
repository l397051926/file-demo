package com.gennlife.fs.common.utils;

import com.gennlife.darren.collection.keypath.KeyPath;

public class KeyPathUtil {

    public static KeyPath toKeyPath(String s) {
        return KeyPath.compile(s);
    }

    public static String toPathString(KeyPath path) {
        return path == null ? null : path.join(".");
    }

    public static String toRedisKey(KeyPath path) {
        return path == null ? null : path.join(":");
    }

}
