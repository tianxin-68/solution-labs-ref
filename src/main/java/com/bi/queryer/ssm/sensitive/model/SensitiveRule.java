package com.bi.queryer.ssm.sensitive.model;

/**
 * @Author contributor
 * @Date 15:19 2025/11/12
 * @Description 敏感规则：数据内容若匹配上规则表达式，则敏感，否则不敏感
 **/
public class SensitiveRule {
    private String ruleCode;
    private String ruleExpression;
    private String ruleDesc;
    private String sensitiveLevel;
    private String sensitiveDesc;

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleExpression() {
        return ruleExpression;
    }

    public void setRuleExpression(String ruleExpression) {
        this.ruleExpression = ruleExpression;
    }

    public String getRuleDesc() {
        return ruleDesc;
    }

    public void setRuleDesc(String ruleDesc) {
        this.ruleDesc = ruleDesc;
    }

    public String getSensitiveLevel() {
        return sensitiveLevel;
    }

    public void setSensitiveLevel(String sensitiveLevel) {
        this.sensitiveLevel = sensitiveLevel;
    }

    public String getSensitiveDesc() {
        return sensitiveDesc;
    }

    public void setSensitiveDesc(String sensitiveDesc) {
        this.sensitiveDesc = sensitiveDesc;
    }
}
