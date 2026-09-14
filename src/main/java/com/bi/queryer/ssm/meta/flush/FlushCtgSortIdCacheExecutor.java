package com.bi.queryer.ssm.meta.flush;

import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;

import java.util.List;
import java.util.Map;

public class FlushCtgSortIdCacheExecutor extends BaseFlushCacheExecutor {

    FlushCtgSortIdCacheExecutor(Map<String, ?> initParams){
        super(initParams);
    }

    @Override
    public void execute() {

        String data = BIUtil.nvl(initParams.get("categoryList"),"");
        List<MetaFieldCategory> categoryList = JSON.parseArray(data, MetaFieldCategory.class);

        for (MetaFieldCategory metaFieldCategory : categoryList) {
            MetaFieldCategory cacheCategory = SSDMetaCacheManager.getCategoryById(metaFieldCategory.getId());
            if (cacheCategory != null) {
                cacheCategory.setShowOrder(metaFieldCategory.getShowOrder());
            }
        }

    }


}
