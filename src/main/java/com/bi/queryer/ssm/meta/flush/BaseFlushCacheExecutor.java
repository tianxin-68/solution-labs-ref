package com.bi.queryer.ssm.meta.flush;

import java.util.HashMap;
import java.util.Map;

public class BaseFlushCacheExecutor implements FlushCacheExecutor {

    protected Map<String, ?> initParams = new HashMap<>();

    BaseFlushCacheExecutor(Map<String, ?> initParams) {
        this.initParams = initParams;
    }

    @Override
    public void execute() {

    }
}
