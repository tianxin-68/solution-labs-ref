package com.bi.queryer.ssm.engine.validator;

import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.util.FieldUtil;
import com.bi.queryer.util.BIUtil;

import java.util.*;

/**
 * @Author contributor
 * @Date 17:15 2024-04-12
 * @Description 查询配置验证器：校验合法性
 **/
public class QueryConfigureValidator {

    public static ValidateResult validate(QueryConfigure config){
        ValidateResult result = new ValidateResult();
        List<QueryField> allFields = config.getAllFields();

        if(BIUtil.isEmpty(allFields)){
            return result;
        }

        List<MetaField> metaFields = new ArrayList<>();

        Map<String, String> queryFieldTitles = new HashMap<>();
        for(QueryField f : allFields){
           if(f.isLodField() || f.getMeta() == null){
               continue;
           }
           metaFields.add(f.getMeta());

            String fieldTitle = BIUtil.isEmpty(f.getDisplayTitle()) ? f.getMeta().getTitle() : f.getDisplayTitle();
            queryFieldTitles.put(f.getId(), fieldTitle);
        }

        if(BIUtil.isEmpty(metaFields)){
            return result;
        }

        List<String> testFieldIds = new ArrayList<>();
        testFieldIds.add(metaFields.get(0).getId());
        Set<String> excludeIds = FieldUtil.getExcludeFieldById(testFieldIds, metaFields);
        if(BIUtil.isEmpty(excludeIds)){
            return result;
        }

        result.setSuccess(false);
        List<String> messageList = new ArrayList<>();
        for(String excludeId : excludeIds){
            String title = queryFieldTitles.get(excludeId);
            if(BIUtil.isNotEmpty(title)){
                messageList.add(title);
            }
        }
        String excludeMessage = BIUtil.listToStr(messageList);
        String sourceTitle = queryFieldTitles.get(testFieldIds.get(0));
        if(BIUtil.isNotEmpty(sourceTitle) && BIUtil.isNotEmpty(excludeMessage)){
            excludeMessage = String.format("查询配置中[%s]与[%s]不可同时查询，请调整查询字段。", sourceTitle, excludeMessage);
        }
        result.setMessage(excludeMessage);

        return result;
    }
}
