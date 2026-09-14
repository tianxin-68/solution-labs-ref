package com.bi.queryer.ssm.query.log;

import com.bi.queryer.ssm.engine.config.field.FieldValue;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;

import java.util.stream.Collectors;

/**
 * @Author contributor
 * @Date 10:42 2025/11/7
 * @Description TODO
 **/
public class SSDQueryCfgFieldLogEntity extends SSDQueryFieldLogEntity{

    public SSDQueryCfgFieldLogEntity(){

    }

    public SSDQueryCfgFieldLogEntity(UIQueryField f){
        if(f == null) {
            return;
        }
        this.fieldId = f.getId();
        this.fieldCode = f.getCode();
        this.fieldTitle = f.getTitle();
        this.fieldName = f.getName();
        this.fieldDesc = f.getTitle();
        this.moduleId = f.getModuleCtgId();
        this.ctgId = f.getCtgId();
        this.isResult = f.isInResultArea() ? Enabled.YES.getId() : Enabled.NO.getId();
        this.isFilter = f.isInFilterArea() && BIUtil.isNotEmpty(f.getValues()) ? Enabled.YES.getId() : Enabled.NO.getId();

        // 过滤值：最多10个
        String values = null;
        if(BIUtil.isNotEmpty(f.getValues())){
            values = BIUtil.listToStr(f.getValues().stream().map(FieldValue::getId).limit(10).collect(Collectors.toList()));
        }
        this.filterValues = values;
    }
}
