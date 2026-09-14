package com.bi.queryer.ssm.meta.flush;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSON;

import java.util.List;
import java.util.Map;

public class FlushFieldByCodeCacheExecutor extends BaseFlushCacheExecutor {

    FlushFieldByCodeCacheExecutor(Map<String, ?> initParams){
        super(initParams);
    }

    @Override
    public void execute() {

        String data = BIUtil.nvl(initParams.get("metaField"), "[]");

        List<MetaField> metaFields = JSON.parseArray(data, MetaField.class);
        for (MetaField metaField : metaFields) {
            List<MetaField> metaFieldList = SSDMetaCacheManager.getFieldByCode(metaField.getCode());

            if (CollUtil.isNotEmpty(metaFieldList)) {
                for (MetaField field : metaFieldList) {
                    //field.setIsSensitive(metaField.getIsSensitive());
                    //field.setSensitiveDataType(metaField.getSensitiveDataType());
                    field.setTitle(metaField.getTitle());
                    field.setExportAppendString(metaField.getExportAppendString());
                    field.setFilterShowType(metaField.getFilterShowType());

                    if ("true".equalsIgnoreCase(SC.v("set.field.sensitive.level.by.mgp.enable" , "true"))) {
                        field.setSensitiveLevel(metaField.getSensitiveLevel());
                        field.setSensitiveDataType(metaField.getSensitiveDataType());
                    }

                    if(StrUtil.isNotEmpty(metaField.getAggExpression())){
                        field.setAggExpression(metaField.getAggExpression());
                    }

                    field.setShowFormatExpression(metaField.getShowFormatExpression());
                    field.setFilterSQL(metaField.getFilterSQL());
                    field.setDataType(metaField.getDataType());
                }
            }

            SSDMetaCacheManager.setFieldValueMapByFieldCode(metaField.getKpiNo(), metaField.getDimValueMap());
        }
    }
}
