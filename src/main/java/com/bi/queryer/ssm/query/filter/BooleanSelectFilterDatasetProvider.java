package com.bi.queryer.ssm.query.filter;

import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.sys.enums.Enabled;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: contributor
 * Date: 2020/2/6
 * Time: 12:45
 * Description: 布尔下来框
 */
@Service
@Scope("prototype")
public class BooleanSelectFilterDatasetProvider implements IFilterDatasetProvider{
    @Override
    public List<? extends Object> buildDataset(MetaField metaField, Map<String, Object> params) {
        List<Map<String, String>> result = new ArrayList<>();
        for(Enabled e : Enabled.values()){
            Map<String, String> item = new HashMap<>();
            item.put("id", e.getId() + "");
            item.put("name", e.getDesc() + "");
            result.add(item);
        }
        return result;
    }
}
