package com.bi.queryer.ssm.engine.session;

/**
 * @Author contributor
 * @Date 17:31 2024-03-27
 * @Description 查询session选项db实体
 **/
public class QuerySessionSetting {
    private String scope;

    private String userName;

    private String templateId;

    private String optionCode;

    private String optionValue;

    private String optionDesc;

    private String supportQueryEngines;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getOptionCode() {
        return optionCode;
    }

    public void setOptionCode(String optionCode) {
        this.optionCode = optionCode;
    }

    public String getOptionDesc() {
        return optionDesc;
    }

    public void setOptionDesc(String optionDesc) {
        this.optionDesc = optionDesc;
    }

    public String getSupportQueryEngines() {
        return supportQueryEngines;
    }

    public void setSupportQueryEngines(String supportQueryEngines) {
        this.supportQueryEngines = supportQueryEngines;
    }
}
