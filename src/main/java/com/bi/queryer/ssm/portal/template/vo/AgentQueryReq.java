package com.bi.queryer.ssm.portal.template.vo;

import java.util.ArrayList;
import java.util.List;

public class AgentQueryReq {

    /**
     * 解读类型
     */
    private String interpretType;

    private String template;

    /**
     * 模型名
     */
    private String modelName;

    /**
     * 数据上下文
     * 例如：全局筛选器的时间范围
     */
    private String dataContext;

    /**
     * 用户提示词
     */
    private String userIntention;

    /**
     * 输出格式
     */
    private String outputFormat;

    /**
     * 脚本代码
     */
    private String scriptCode;

    /**
     * 数据集
     */
    private List<AgentQueryDataSet> dataSets = new ArrayList<>();

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }


    public List<AgentQueryDataSet> getDataSets() {
        return dataSets;
    }

    public void setDataSets(List<AgentQueryDataSet> dataSets) {
        this.dataSets = dataSets;
    }

    public String getScriptCode() {
        return scriptCode;
    }

    public void setScriptCode(String scriptCode) {
        this.scriptCode = scriptCode;
    }

    public String getInterpretType() {
        return interpretType;
    }

    public void setInterpretType(String interpretType) {
        this.interpretType = interpretType;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getDataContext() {
        return dataContext;
    }

    public void setDataContext(String dataContext) {
        this.dataContext = dataContext;
    }

    public String getUserIntention() {
        return userIntention;
    }

    public void setUserIntention(String userIntention) {
        this.userIntention = userIntention;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
}
