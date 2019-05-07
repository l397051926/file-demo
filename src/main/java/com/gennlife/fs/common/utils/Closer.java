package com.gennlife.fs.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Created by chensong on 2015/12/14.
 */
public class Closer {
    private static final Logger log = LoggerFactory.getLogger(Closer.class);
    private Closeable[] cs;

    public Closer(Closeable... cs) {
        this.cs = cs;
    }

    public static void close(Closeable... cs) {
        if (cs == null)
            return;
        for (Closeable c : cs) {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable e) {
                    log.warn("资源关闭时出错:" + c.getClass().getName(), e);
                }
            }
        }
    }

    public static void close(ResultSet... cs) {
        if (cs == null)
            return;
        for (ResultSet c : cs) {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable e) {
                    log.warn("资源关闭时出错:" + c.getClass().getName(), e);
                }
            }
        }
    }

    public static void close(Statement... cs) {
        if (cs == null)
            return;
        for (Statement c : cs) {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable e) {
                    log.warn("资源关闭时出错:" + c.getClass().getName(), e);
                }
            }
        }
    }


    public static void close(Iterable<Closeable> cs) {
        if (cs == null)
            return;
        for (Closeable c : cs) {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable e) {
                    log.warn("资源关闭时出错:" + c.getClass().getName(), e);
                }
            }
        }
    }

    public void recycle() {
        close(cs);
        cs = null;
    }
}
