package com.gennlife.fs.common.response.paging;

/**
 * Created by Chenjinfeng on 2016/11/22.
 */
public interface PagingInterface<T> {
    public boolean hasNext();
    public T next();
    public long getTotal();
}
