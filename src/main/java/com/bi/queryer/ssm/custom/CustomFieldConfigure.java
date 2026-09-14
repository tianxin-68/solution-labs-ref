package com.bi.queryer.ssm.custom;

import com.bi.queryer.ssm.engine.lod.LodUIConfigure;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义字段配置
 */
public class CustomFieldConfigure {
    // 类型
    private String type = CustomFieldType.COMMON.getCode();

    private Integer enable = null;
    private Integer isMeasure = null;
    private String expression = null;

    private String rawExpression = null;
    // 映射用于字段id与字段标题的匹配
    private List<CustomFieldExpressionIdMapping> expressionIdMapping = new ArrayList<>();

    //是否展示百分比
    private Boolean isRatio;

    //小数点位数
    private Integer decimal;

    private LodUIConfigure lodConfig = new LodUIConfigure();

    public Integer getEnable() {
        return enable;
    }

    public void setEnable(Integer enable) {
        this.enable = enable;
    }

    public Integer getIsMeasure() {
        return isMeasure;
    }

    public void setIsMeasure(Integer isMeasure) {
        this.isMeasure = isMeasure;
    }

    public String getExpression() {
        if(BIUtil.isNotEmpty(expression) && expression.trim().startsWith("=")){
            expression = expression.trim().replaceFirst("=", "");
        }
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public List<CustomFieldExpressionIdMapping> getExpressionIdMapping() {
        return expressionIdMapping;
    }

    public void setExpressionIdMapping(List<CustomFieldExpressionIdMapping> expressionIdMapping) {
        this.expressionIdMapping = expressionIdMapping;
    }

    public String getMappingTitle(String id) {
        if(BIUtil.isEmpty(id) || BIUtil.isEmpty(expressionIdMapping)) {
            return "";
        }
        for(CustomFieldExpressionIdMapping m : expressionIdMapping){
            if(id.equals(m.getId())) {
                return m.getTitle();
            }
        }
        return "";
    }

    public boolean isEmpty(){
        return BIUtil.isEmpty(expression);
    }

    public Boolean getRatio() {
        return isRatio;
    }

    public void setRatio(Boolean ratio) {
        isRatio = ratio;
    }

    public Integer getDecimal() {
        return decimal;
    }

    public void setDecimal(Integer decimal) {
        this.decimal = decimal;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LodUIConfigure getLodConfig() {
        return lodConfig;
    }

    public void setLodConfig(LodUIConfigure lodConfig) {
        this.lodConfig = lodConfig;
    }

    public String getRawExpression() {
        return rawExpression;
    }

    public void setRawExpression(String rawExpression) {
        this.rawExpression = rawExpression;
    }
}
