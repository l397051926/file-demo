package com.gennlife.fs.common.response.paging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Chenjinfeng on 2016/11/22.
 */
public abstract class PagingBaseABS<T> implements PagingInterface<T> {
    protected final Logger logger= LoggerFactory.getLogger(this.getClass());
    protected int page=0;
    protected long page_size=1;
    protected long total=0;
    protected int maxpage=0;
    @Override
    public long getTotal()
    {
        return total;
    }
    @Override
    public  boolean hasNext()
    {
        if(total<=0)return false;
        return maxpage>=(page+1);
    }
}
