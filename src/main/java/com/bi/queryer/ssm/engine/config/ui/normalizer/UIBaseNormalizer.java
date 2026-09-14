package com.bi.queryer.ssm.engine.config.ui.normalizer;

import com.bi.queryer.ssm.custom.CustomFieldConfigure;
import com.bi.queryer.ssm.custom.CustomFieldParser;
import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author contributor
 * @Date 16:07 2025/1/6
 * @Description TODO
 **/
public abstract class UIBaseNormalizer {
    protected UIQueryConfigure uiQueryConfigure;

    public UIBaseNormalizer(UIQueryConfigure uiQueryConfigure) {
        this.uiQueryConfigure = uiQueryConfigure;
    }

    public abstract void normalize();

    public UIQueryConfigure getUiQueryConfigure() {
        return uiQueryConfigure;
    }

    public void setUiQueryConfigure(UIQueryConfigure uiQueryConfigure) {
        this.uiQueryConfigure = uiQueryConfigure;
    }

    protected UIQueryField createFieldById(String fieldId){
        MetaField metaField = SSDMetaCacheManager.getField(fieldId);
        if(metaField == null){
            return null;
        }
        UIQueryField uiField = new UIQueryField();
        this.copy(metaField, uiField);
        return uiField;
    }

    protected boolean isCalcField(UIQueryField field){
        boolean isCalc = field.getCustomFieldConfigure() != null && BIUtil.isNotEmpty(field.getCustomFieldConfigure().getExpression());
        return isCalc;
    }

    protected List<String> getCalcRefFieldIdList(UIQueryField calcField){
        List<String> refFieldIdList = new ArrayList<>();
        if(!isCalcField(calcField)){
            return refFieldIdList;
        }

        CustomFieldConfigure customFieldConfigure = calcField.getCustomFieldConfigure();
        String expression = customFieldConfigure.getExpression();
        Pattern pattern = Pattern.compile(CustomFieldParser.expressionPattern);
        Matcher matcher = pattern.matcher(expression);
        while (matcher.find()){
            String fieldId = matcher.group();
            refFieldIdList.add(fieldId);
        }
        return refFieldIdList;
    }

    public void copy(MetaField metaField, UIQueryField uiQueryField){
        uiQueryField.setId(metaField.getId());
        uiQueryField.setName(metaField.getName());
        uiQueryField.setTitle(metaField.getTitle());
        uiQueryField.setCode(metaField.getCode());
        uiQueryField.setFilterValueType(metaField.getFilterShowType());
        uiQueryField.setCtgDataType(metaField.getDataType());
        if(BIUtil.isNotEmpty(metaField.getModuleCtgId())){
            uiQueryField.setModuleCtgId(metaField.getModuleCtgId());
        }else {
            String ctgId = metaField.getCategoryId();
            if (BIUtil.isNotEmpty(ctgId)) {
                ctgId = ctgId.split(",")[0];
                MetaFieldCategory ctg = SSDMetaCacheManager.getCategoryById(ctgId);
                if (ctg != null) {
                    uiQueryField.setModuleCtgId(ctg.getModuleCtgId());
                }
            }
        }
    }
}
