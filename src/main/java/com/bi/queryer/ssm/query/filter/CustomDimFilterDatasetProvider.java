package com.bi.queryer.ssm.query.filter;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.util.BIMap;
import com.bi.queryer.util.BIUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 自定义维度过滤器数据源提供者
 */
@Component
@Scope("prototype")
public class CustomDimFilterDatasetProvider implements IFilterDatasetProvider{
    @Override
    public Object buildDataset(MetaField metaField, Map<String, Object> params) {

        MultiSelectFilterResult result = new MultiSelectFilterResult();

        String fieldValues = BIUtil.nvl(params.get("fieldValues"),"");
        if(StrUtil.isEmpty(fieldValues)){
            return result;
        }

        //枚举值信息，用逗号，顿号，分号，回车或空格分隔
        fieldValues = StringUtils.replaceEach(fieldValues, new String[]{"\n", ";", "；"}, new String[]{",", ",", ","});
        String[] fieldValueList = fieldValues.split(",");

        for(String fieldValue : fieldValueList){
            BIMap row = new BIMap();
            row.put("id", fieldValue);
            row.put("name", fieldValue);
            result.getRows().add(row);
        }

        result.setTotal(result.getRows().size());

        return result;
    }
}
