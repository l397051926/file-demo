package com.gennlife.fs.common.response.paging;

import com.gennlife.fs.common.model.QueryParam;
import com.gennlife.fs.common.model.QueryResult;
import com.gennlife.fs.common.utils.HttpRequestUtils;
import com.gennlife.fs.common.utils.StringUtil;
import com.gennlife.fs.system.bean.BeansContextUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Created by Chenjinfeng on 2016/11/9.
 */
public class PagingResult extends PagingBaseABS<JsonArray> {
    private QueryParam queryparam;
    private String scroll_id;
    private boolean export = false;

    public PagingResult(QueryParam qp) {
        this.queryparam = qp;
        this.page = qp.getPage();
        this.page_size = qp.getSize();
        if (page_size <= 0) page_size = 1;
        page = 0;
        queryparam.setPage(1);
        queryparam.setSize(0);
        total = HttpRequestUtils.search(queryparam).getTotal();
        queryparam.setSize(page_size);
        maxpage = (int) Math.ceil(total * 1.0 / page_size);
        logger.info("total " + total + " page " + maxpage + " page_size " + queryparam.getSize());
        export = !StringUtil.isEmptyStr(BeansContextUtil.getUrlBean().getExportUri());
    }

    @Override
    public JsonArray next() {
        page++;
        QueryResult result = null;
        if (!export) {
            queryparam.setPage(page);
            result = HttpRequestUtils.search(queryparam);
        } else {
            if (StringUtil.isEmptyStr(scroll_id))
                queryparam.setPage(page);
            else queryparam.setScrollId(scroll_id);
            result = HttpRequestUtils.export(queryparam);
        }
        scroll_id = result.getScroll_id();
        if (result.getTotal() != total) {
            logger.warn(" total changed  from " + total + " to " + result.getTotal());
            total = result.getTotal();
            maxpage = (int) Math.ceil(total * 1.0 / page_size);
            if(total==0)
            {
                logger.warn("empty search "+getQuery());
            }
            logger.info("total " + total + " page " + maxpage + " page_size " + queryparam.getSize());
        }
        return result.getDatas();
    }

    public JsonObject getQuery() {
        return queryparam.getJson();
    }
}
