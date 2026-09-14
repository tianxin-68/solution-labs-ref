package com.bi.queryer.ssm.portal.template.vo;

import java.util.List;

/**
 * @Auther: contributor
 * @Date: 2026/3/11 10:14
 * @Description:
 */
public class AiScriptGenReq {

    /**
     * 解释类型：template
     */
    private String interpretType;

    /**
     * 模板内容
     */
    private String template;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 数据上下文
     */
    private String dataContext;

    /**
     * 用户意图
     */
    private String userIntention;

    /**
     * 输出格式
     */
    private String outputFormat;

    /**
     * 数据集列表
     */
    private List<AgentQueryDataSet> dataSets;

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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
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

    public List<AgentQueryDataSet> getDataSets() {
        return dataSets;
    }

    public void setDataSets(List<AgentQueryDataSet> dataSets) {
        this.dataSets = dataSets;
    }
}
